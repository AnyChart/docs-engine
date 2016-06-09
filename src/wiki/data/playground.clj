(ns wiki.data.playground
  (:require [wiki.components.jdbc :refer [query one insert! exec]]
            [version-clj.core :refer [version-compare]]
            [honeysql.helpers :refer [select from where]]
            [cheshire.core :refer [generate-string parse-string]]))

(defn project-by-key [jdbc key]
  (one jdbc (-> (select :id :key)
                (from :projects)
                (where [:= :key key]))))

(defn version-by-key [jdbc project-id key]
  (one jdbc (-> (select :key :id :engine_version)
                (from :versions)
                (where [:= :project_id project-id]
                       [:= :hidden false]
                       [:= :key key]))))

(defn sample-by-url [jdbc version-id url]
  (if-let [res (one jdbc (-> (select :*)
                             (from :samples)
                             (where [:= :version_id version-id]
                                    [:= :url url])))]
    (-> res
        (assoc :scripts (parse-string (:scripts res)))
        (assoc :css_libs (parse-string (:css_libs res))))))