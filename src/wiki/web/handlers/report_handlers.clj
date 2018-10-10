(ns wiki.web.handlers.report-handlers
  (:require [wiki.data.versions :as vdata]
            [wiki.generator.analysis.page :as analysis-page]
            [wiki.web.helpers :refer :all]))


(defn report [request]
  (let [version-key (-> request :route-params :version)
        report (vdata/version-report (jdbc request) version-key)]
    {:status  200
     :headers {"Access-Control-Allow-Origin" "*"}
     :body    report}))


(defn report-page [request]
  (let [version-key (-> request :route-params :version)
        report (vdata/version-report (jdbc request) version-key)]
    (analysis-page/page report version-key)))
