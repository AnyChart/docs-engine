(ns wiki.data.versions
  (:require [wiki.components.jdbc :refer [query one insert! exec]]
            [version-clj.core :refer [version-compare]]
            [honeysql.helpers :refer :all]
            [clojure.java.jdbc :as clj-jdbc]
            [cheshire.core :refer [generate-string parse-string]]
            [wiki.data.utils :refer [pg->clj clj->jsonb]]))

;; CREATE SEQUENCE version_id_seq;
;; CREATE TABLE versions (
;;    id integer PRIMARY KEY DEFAULT nextval('version_id_seq'),
;;    key varchar(255) not NULL,
;;    commit varchar(40) not NULL,
;;    hidden BOOLEAN DEFAULT FALSE,
;;    tree TEXT,
;;    zip BYTEA
;; );

(defn add-version [jdbc key commit tree config]
  (:id (first (insert! jdbc :versions {:key    key
                                       :commit commit
                                       :tree   (generate-string tree)
                                       :config (clj->jsonb config)}))))

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
  (sort (comp - version-compare)
        (map :key (query jdbc (-> (select :key)
                                  (from :versions)
                                  (where [:= :hidden false]))))))

(defn versions-full-info [jdbc]
  (sort (comp - #(version-compare (:key %1) (:key %2)))
        (query jdbc (-> (select :id :key)
                        (from :versions)
                        (where [:= :hidden false])))))

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
         sorted-versions (sort (comp - #(version-compare (:key %1) (:key %2))) versions)
         url-versions (map #(assoc % :url (str "/" (:key %)
                                               (when (:url %) (str "/" (:url %)))))
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
        redirects* (->> redirects (filter (comp some? :config))
                        (map (comp :redirects pg->clj :config)))]
    (seq (set (apply concat redirects*)))))