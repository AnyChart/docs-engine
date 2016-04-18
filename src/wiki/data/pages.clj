(ns wiki.data.pages
  (:require [wiki.components.jdbc :refer [one exec query insert!]]
            [honeysql.helpers :refer :all]))

;; CREATE SEQUENCE page_id_seq;
;; CREATE TABLE pages (
;;   id integer PRIMARY KEY DEFAULT nextval('page_id_seq'),
;;   version_id integer references versions(id),
;;   url varchar(255) not null,
;;   full_name varchar(255),
;;   content text,
;;   last_modified bigint
;; );

(defn page-by-url [jdbc version-id page-url]
  (if version-id
    (one jdbc (-> (select :*)
                  (from :pages)
                  (where [:= :version_id version-id]
                         [:= :url page-url])))))
    
(defn pages-urls [jdbc version-id]
  (query jdbc (-> (select :url :last_modified)
                  (from :pages)
                  (where [:= :version_id version-id]))))

(defn delete-version-pages [jdbc version-id]
  (exec jdbc (-> (delete-from :pages)
                 (where [:= :version_id version-id]))))

(defn add-page [jdbc version-id url title content last-modified]
  (insert! jdbc :pages {:url url
                        :content content
                        :full_name title
                        :version_id version-id
                        :last_modified last-modified}))

(defn page-exists? [jdbc version-id url]
  (not (nil? (one jdbc (-> (select :id)
                           (from :pages)
                           (where [:= :url url]
                                  [:= :version_id version-id]))))))

(defn all-pages-by-version [jdbc version-id]
  (query jdbc (-> (select :url :full_name :content)
                  (from :pages)
                  (where [:= :version-id version-id]))))