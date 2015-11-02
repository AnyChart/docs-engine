(ns wiki.data.sitemap
  (:require [clojure.xml :refer [emit emit-element]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [honeysql.helpers :refer :all]
            [wiki.components.jdbc :refer [query one insert! exec]]
            [wiki.data.versions :as vdata]))

;; create table sitemap (
;;  page_url varchar(255),
;;  version_id integer references versions(id),
;;  last_modified bigint
;; );

(def formatter (f/formatter "YYYY-MM-dd'T'hh:mm:ss'Z'"))

(defn remove-by-version [jdbc version-id]
  (exec jdbc (-> (delete-from :sitemap)
                 (where [:= :version_id version-id]))))

(defn- create-sitemap-entry [entry version-id]
  {:page_url (:full-name entry)
   :version_id version-id
   :last_modified (:last-modified entry)})

(defn- generate-version-sitemap [jdbc idx version])

(defn generate-sitemap [jdbc])
