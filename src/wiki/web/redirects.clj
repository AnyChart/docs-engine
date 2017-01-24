(ns wiki.web.redirects
  (:require [clojure.string :as s :refer [split replace]]
             [ring.util.response :refer [redirect]]
            [taoensso.timbre :refer [info]]))

(defn parse-redirects [str-stata]
  (->> (re-seq #"([^>\s]*)\s*>>\s*([^>\s]*)\s*\n" str-stata)
       (map #(drop 1 %))))

(defn get-version [uri]
  (let [uri (if (.startsWith uri "/") (subs uri 1) uri)
        version (first (split uri #"/"))]
    version))

(defn check-redirect [request handler]
  (let [redirects (-> request :component :redirects deref)
        uri (:uri request)
        version (get-version uri)]
    (if (and version (every? #(not= version %) ["_update_" "sitemap" "latest" "_redirects_"]))
      (let [version-redirects (map (fn [coll] (map #(s/replace % "{VERSION}" version) coll)) redirects)
            redirect-uri (second (first (filter #(= uri (first %)) version-redirects)))]
        (if redirect-uri
          (redirect redirect-uri)
          (handler request)))
      (handler request))))

(defn wrap-redirect [handler]
  (fn [request] (check-redirect request handler)))