(ns wiki.web.handlers.sitemap-handlers
  (:require [wiki.data.sitemap :as sitemap]
            [wiki.web.helpers :refer :all]
            [ring.util.response :refer [redirect response content-type file-response header]]))


(defn show-sitemap [request]
  (-> (response (sitemap/generate-sitemap (jdbc request)))
      (content-type "text/xml")))


(defn show-sitemap-version [request]
  (let [version-name (-> request :params :version)]
    (-> (response (sitemap/generate-sitemap-version (jdbc request) version-name))
        (content-type "text/xml"))))
