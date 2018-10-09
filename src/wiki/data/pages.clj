(ns wiki.data.pages
  (:require [wiki.components.jdbc :refer [one exec query insert!]]
            [clojure.java.jdbc :as clj-jdbc]
            [honeysql.helpers :refer :all]
            [honeysql.types :as types]
            [wiki.data.utils :refer [pg->clj clj->jsonb]]))


(defn page-by-url [jdbc version-id page-url]
  (if version-id
    (when-let [page (one jdbc (-> (select :*)
                                  (from :pages)
                                  (where [:= :version_id version-id]
                                         [:= :url page-url])))]
      (-> page
          (clojure.core/update :tags #(when % (vec (.getArray %))))
          (clojure.core/update :config #(when % (pg->clj %)))))))


(defn pages-urls [jdbc version-id]
  (query jdbc (-> (select :url :last_modified)
                  (from :pages)
                  (where [:= :version_id version-id]))))


(defn delete-version-pages [jdbc version-id]
  (exec jdbc (-> (delete-from :pages)
                 (where [:= :version_id version-id]))))


(defn- to-insert-vals [jdbc v]
  (let [con (clj-jdbc/get-connection (:conn jdbc))
        arr (.createArrayOf con "varchar" (into-array v))]
    (.close con)
    arr))


(defn add-page [jdbc version-id url title content last-modified tags config]
  (insert! jdbc :pages {:url           url
                        :content       content
                        :full_name     title
                        :version_id    version-id
                        :last_modified last-modified
                        :tags          (to-insert-vals jdbc tags)
                        :config        (clj->jsonb config)}))


(defn page-exists? [jdbc version-id url]
  (not (nil? (one jdbc (-> (select :id)
                           (from :pages)
                           (where [:= :url url]
                                  [:= :version_id version-id]))))))


(defn all-pages-by-version [jdbc version-id]
  (query jdbc (-> (select :url :full_name :content)
                  (from :pages)
                  (where [:= :version-id version-id]))))


(defn links [jdbc version-name]
  (let [data (query jdbc (-> (select :url :full_name :config)
                             (from :pages)
                             (where [:= :version_id
                                     (-> (select :id)
                                         (from :versions)
                                         (where [:= :key version-name]))])))]
    (map (fn [page]
           (clojure.core/update page :config #(when % (pg->clj %))))
         data)))