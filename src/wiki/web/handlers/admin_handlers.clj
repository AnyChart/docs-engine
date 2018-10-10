(ns wiki.web.handlers.admin-handlers
  (:require [wiki.components.redis :as redisca]
            [wiki.data.versions :as vdata]
            [wiki.web.helpers :refer :all]
            [taoensso.timbre :as timbre]
            [wiki.views.admin.admin-page :as admin-view]
            [ring.util.response :refer [redirect response content-type file-response header]]
            [clojure.string :as string]))


(defn admin-page [request]
  (let [versions (vdata/versions-full-info (jdbc request))]
    (admin-view/page {:title-prefix "Admin Panel | AnyChart Documentation\""
                      :description  "Admin Panel page"
                      :commit       (:commit (config request))} versions)))


(defn redirects-page [request]
  (let [redirects (-> request :component :redirects deref)]
    (if (empty? redirects)
      (response "Redirects empty")
      (response
        (->> redirects
             (sort-by first)
             (map #(str (first %) "\t >> \t" (second %)))
             (string/join "\n"))))))


;; API
(defn delete-version [request]
  (let [version-key (-> request :params :version)]
    (timbre/info "DELETE version request:" version-key)
    (vdata/remove-branch-by-key (jdbc request) version-key)
    (redirect "/_admin_")))


(defn rebuild-version [request]
  (let [params (-> request :params)]
    (timbre/info "REBUILD version request:" params)
    ;; just for not showing updated version in select on admin panel
    (when-let [version (:version params)]
      (vdata/remove-branch-by-key (jdbc request) version))
    (redisca/enqueue (redis request)
                     (-> request :component :config :queue)
                     (assoc params :cmd "generate"))))


(defn update-versions [request]
  (redisca/enqueue (redis request)
                   (-> request :component :config :queue)
                   {:cmd "generate"}))