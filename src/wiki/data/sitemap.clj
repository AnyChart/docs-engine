(ns wiki.data.sitemap
  (:require [clojure.xml :refer [emit emit-element]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [honeysql.helpers :refer :all]
            [wiki.components.jdbc :refer [query one insert! exec]]
            [wiki.data.versions :as vdata]
            [wiki.data.pages :refer [pages-urls]]))

(def formatter (f/formatter "YYYY-MM-dd'T'hh:mm:ss'Z'"))

(defn- get-priority [idx]
  (cond
    (> idx 5) 0.1
    :else (- 0.6 (/ idx 10))))

(defn- generate-version-sitemap [jdbc idx version]
  (let [priority (get-priority idx)
        entries (pages-urls jdbc (:id version))]
    (map (fn [entry]
           {:tag :url
            :content [{:tag :loc :content
                       [(str "https://docs.anychart.com/" (:key version) "/"
                             (:url entry))]}
                      {:tag :priority :content [(format "%.1f" priority)]}
                      {:tag :lastmod :content [(f/unparse formatter
                                                          (c/from-long (* 1000 (:last_modified entry))))]}]})
         entries)))

(defn- landing-entry [])

(defn generate-sitemap [jdbc]
  (with-out-str
    (emit {:tag :urlset :attrs {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
           :content (apply concat (map-indexed (fn [idx val]
                                                 (generate-version-sitemap jdbc idx val))
                                               (vdata/versions-full-info jdbc)))})))
