(ns wiki.web.handlers.sitemap-handlers
  (:require [wiki.web.helpers :refer :all]
            [wiki.data.versions :as vdata]
            [wiki.data.pages :as pages-data]
            [wiki.config.core :as c]
            [wiki.util.utils :as utils]
            [ring.util.response :refer [redirect response content-type file-response header]]
            [clojure.xml :refer [emit emit-element]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as ct]))


(def formatter (f/formatter "YYYY-MM-dd'T'hh:mm:ss'Z'"))


(defn get-priority [idx]
  (cond
    (> idx 5) 0.1
    :else (- 0.6 (/ idx 10))))


(defn- generate-version-sitemap [jdbc idx version & [show-version]]
  (let [priority (get-priority idx)
        entries (pages-data/pages-urls jdbc (:id version))]
    (map (fn [entry]
           {:tag     :url
            :content [{:tag :loc :content
                            [(str (c/domain)
                                  (when show-version (str (:key version) "/"))
                                  (utils/escape-url (:url entry)))]}
                      {:tag :priority :content [(format "%.1f" priority)]}
                      {:tag :changefreq :content ["monthly"]}
                      {:tag :lastmod :content [(f/unparse formatter
                                                          (ct/from-long (* 1000 (:last_modified entry))))]}]})
         entries)))


(defn generate-sitemap [jdbc]
  (with-out-str
    (emit {:tag     :urlset :attrs {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
           :content (generate-version-sitemap jdbc 0 (first (vdata/versions-full-info jdbc)))})))


(defn generate-sitemap-version [jdbc version-name]
  (let [versions (vdata/versions-full-info jdbc)
        version (first (filter #(= version-name (:key %)) versions))]
    (with-out-str
      (emit {:tag     :urlset :attrs {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
             :content (generate-version-sitemap jdbc 0 version true)}))))


(defn show-sitemap [request]
  (-> (response (generate-sitemap (jdbc request)))
      (content-type "text/xml")))


(defn show-sitemap-version [request]
  (let [version-name (-> request :params :version)]
    (-> (response (generate-sitemap-version (jdbc request) version-name))
        (content-type "text/xml"))))
