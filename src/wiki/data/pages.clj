(ns wiki.data.pages
  (:require [wiki.components.jdbc :refer [one exec insert!]]
            [honeysql.helpers :refer :all]))

;; CREATE SEQUENCE page_id_seq;
;; CREATE TABLE pages (
;;   id integer PRIMARY KEY DEFAULT nextval('page_id_seq'),
;;   version_id integer references versions(id),
;;   url varchar(255) not null,
;;   full_name varchar(255),
;;   content text
;; );

(defn page-by-url [jdbc version-id page-url]
  (if version-id
    (one jdbc (-> (select :*)
                  (from :pages)
                  (where [:= :version_id version-id]
                         [:= :url page-url])))))
    

(defn delete-version-pages [jdbc version-id]
  (exec jdbc (-> (delete-from :pages)
                 (where [:= :version_id version-id]))))

(defn add-page [jdbc version-id url title content]
  (insert! jdbc :pages {:url url
                        :content content
                        :full_name title
                        :version_id version-id}))

(defn page-exists? [jdbc version-id url]
  (not (nil? (one jdbc (-> (select :id)
                           (from :pages)
                           (where [:= :url url]
                                  [:= :version_id version-id]))))))
