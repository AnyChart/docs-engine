(ns wiki.web.redirects
  (:require [clojure.string :as s :refer [split]]
            [ring.util.response :refer [redirect]]
            [toml.core :as toml]
            [taoensso.timbre :refer [info]]
            [clojure.java.io :as io]))

(defn parse-redirects [str-stata]
  (->> str-stata
       clojure.string/trim
       (re-seq #"([^>\s]*)\s*>>\s*([^>\s]*)\s*\n")
       (map #(drop 1 %))))

(defn parse-config [str-data]
  (let [data (toml/read str-data :keywordize)]
    (update-in data [:redirect :redirects] parse-redirects)))

(defn get-config [file-path]
  (prn file-path)
  (when (.exists (io/file file-path))
    (-> file-path
        slurp
        parse-config)))


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