(ns wiki.data.versions
  (:require [wiki.components.jdbc :refer [query one insert! exec]]
            [honeysql.helpers :refer :all :as honey]
            [clojure.java.jdbc :as clj-jdbc]
            [cheshire.core :refer [generate-string parse-string]]
            [wiki.data.utils :refer [pg->clj clj->jsonb]]
            [wiki.util.utils :as utils]
            [wiki.data.pages :as pdata]
            [wiki.data.folders :as fdata]))


(defn add-version [jdbc key commit tree config]
  (:id (first (insert! jdbc :versions {:key    key
                                       :commit commit
                                       :tree   (generate-string tree)
                                       :config (clj->jsonb config)}))))


(defn add-report [jdbc version-id report]
  (exec jdbc (-> (honey/update :versions)
                 (sset {:report (clj->jsonb report)})
                 (where [:= :id version-id]))))


(defn version-by-key [jdbc key]
  (one jdbc (-> (select :key :id)
                (from :versions)
                (where [:= :hidden false]
                       [:= :key key]))))


(defn version-by-id [jdbc version-id]
  (one jdbc (-> (select :key :id)
                (from :versions)
                (where [:= :hidden false]
                       [:= :id version-id]))))


(defn version-report [jdbc version-key]
  (let [res (one jdbc (-> (select :report)
                          (from :versions)
                          (where [:= :key version-key])))]
    (when (:report res)
      (pg->clj (:report res)))))


(defn delete-by-key [jdbc key]
  (exec jdbc (-> (delete-from :versions)
                 (where [:= :key key]))))


(defn delete-by-id [jdbc id]
  (exec jdbc (-> (delete-from :versions)
                 (where [:= :id id]))))


(defn version-ids [jdbc key]
  (map :id (query jdbc (-> (select :id)
                           (from :versions)
                           (where [:= :key key])))))


(defn get-version-tree [jdbc version]
  (if-let [tree (:tree (one jdbc (-> (select :tree)
                                     (from :versions)
                                     (where [:= :id (:id version)]))))]
    (parse-string tree :keywordize-keys true)))


(defn versions [jdbc]
  (->> (query jdbc (-> (select :key)
                       (from :versions)
                       (where [:= :hidden false])))
       (map :key)
       utils/sort-versions))


(defn versions-full-info [jdbc]
  (->> (query jdbc (-> (select :id :key)
                       (from :versions)
                       (where [:= :hidden false])))
       (utils/sort-versions :key)))


(defn outdated-versions-ids [jdbc actual-ids]
  (map :id (query jdbc (-> (select :id)
                           (from :versions)
                           (where [:not [:in :id actual-ids]])))))


(defn remove-versions [jdbc ids]
  (if (seq ids)
    (exec jdbc (-> (delete-from :versions)
                   (where [:in :id ids])))))


(defn default [jdbc]
  (first (versions jdbc)))


(defn need-rebuild? [jdbc version-key commit]
  (nil? (one jdbc (-> (select :key)
                      (from :versions)
                      (where [:= :commit commit]
                             [:= :key version-key])))))


(defn tree-data [jdbc version-id]
  (parse-string (:tree (one jdbc (-> (select :tree)
                                     (from :versions)
                                     (where [:= :id version-id]
                                            [:= :hidden false]))))
                true))


(defn update-zip [jdbc version-id fis]
  (let [conn (-> jdbc :conn :datasource .getConnection)
        stmt (clj-jdbc/prepare-statement conn "UPDATE versions SET zip=? WHERE id=?")]
    (.setBinaryStream stmt 1 fis (.available fis))
    (.setInt stmt 2 version-id)
    (.executeUpdate stmt)
    (.close stmt)
    (.close conn)))


(defn get-zip [jdbc version-id]
  (when-let [hex (:zip (one jdbc (-> (select (honeysql.types/raw "encode(zip, 'hex') as zip"))
                                     (from :versions)
                                     (where [:= :id version-id]
                                            [:= :hidden false]))))]
    (javax.xml.bind.DatatypeConverter/parseHexBinary hex)))


(defn get-page-versions
  ([jdbc url short-url]
   (let [versions (query jdbc (-> (select :versions.key :versions.id, :v.url)
                                  (from :versions)
                                  (left-join [(-> (select :versions.key :versions.id :pages.url)
                                                  (from :versions)
                                                  (join :pages [:= :versions.id :pages.version_id])
                                                  (where [:and [:or [:= :pages.url url] [:= :pages.url short-url]] [:= :versions.hidden false]])) :v]
                                             [:= :versions.id :v.id])))
         sorted-versions (utils/sort-versions :key versions)
         url-versions (map #(assoc % :url (str "/" (:key %)
                                               (when (:url %)
                                                 (str "/" (utils/escape-url (:url %))))))
                           sorted-versions)]
     url-versions))
  ([jdbc url]
   (get-page-versions jdbc url url)))


(defn current-version [version-key versions]
  (first (filter #(= version-key (:key %)) versions)))


(defn get-redirects [jdbc]
  (let [redirects (query jdbc (-> (select :config)
                                  (from :versions)
                                  (where [:= :hidden false])))
        redirects* (->> redirects
                        (filter (comp some? :config))
                        (map (comp :redirects :redirect pg->clj :config)))]
    (seq (set (apply concat redirects*)))))


(defn remove-branch-by-id [jdbc version-id]
  (pdata/delete-version-pages jdbc version-id)
  (fdata/delete-version-folders jdbc version-id)
  (delete-by-id jdbc version-id))


(defn remove-branch-by-key [jdbc version-key]
  (let [version-id (:id (version-by-key jdbc version-key))]
    (remove-branch-by-id jdbc version-id)))