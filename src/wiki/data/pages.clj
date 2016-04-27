(ns wiki.data.pages
  (:require [wiki.components.jdbc :refer [one exec query insert!]]
            [clojure.java.jdbc :as clj-jdbc]
            [honeysql.helpers :refer :all]
            [honeysql.types :as types]))

;; CREATE SEQUENCE page_id_seq;
;; CREATE TABLE pages (
;;   id integer PRIMARY KEY DEFAULT nextval('page_id_seq'),
;;   version_id integer references versions(id),
;;   url varchar(255) not null,
;;   full_name varchar(255),
;;   content text,
;;   last_modified biging,
;;   tags varchar(255)[]
;; );

(defn page-by-url [jdbc version-id page-url]
  (if version-id
    (let [page (one jdbc (-> (select :*)
                             (from :pages)
                             (where [:= :version_id version-id]
                                    [:= :url page-url])))]
      (if (:tags page)
        (clojure.core/update page :tags #(vec (.getArray %)))
        page))))
    
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

(defn add-page [jdbc version-id url title content last-modified tags]
  (insert! jdbc :pages {:url url
                        :content content
                        :full_name title
                        :version_id version-id
                        :last_modified last-modified
                        :tags (to-insert-vals jdbc tags)}))

(defn page-exists? [jdbc version-id url]
  (not (nil? (one jdbc (-> (select :id)
                           (from :pages)
                           (where [:= :url url]
                                  [:= :version_id version-id]))))))
