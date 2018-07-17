(ns wiki.data.folders
  (:require [wiki.components.jdbc :refer [one exec insert!]]
            [honeysql.helpers :refer :all]
            [clojure.string :as string]))


(defn add-folder [jdbc version-id url default-page]
  (insert! jdbc :folders {:version_id   version-id
                          :url          url
                          :default_page default-page}))


(defn delete-version-folders [jdbc version-id]
  (exec jdbc (-> (delete-from :folders)
                 (where [:= :version_id version-id]))))


(defn get-folder-by-url [jdbc version-id url]
  (if (some? version-id)
    (-> (one jdbc (-> (select :url :default_page)
                      (from :folders)
                      (where [:= :version_id version-id]
                             [:= :url (-> url
                                          (string/replace #"^/" "")
                                          (string/replace #"/$" ""))]))))))


(defn folder-exists? [jdbc version-id url]
  (some? (get-folder-by-url jdbc version-id url)))
