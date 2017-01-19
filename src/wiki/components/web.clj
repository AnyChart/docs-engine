(ns wiki.components.web
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :as server]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [taoensso.timbre :as timbre :refer [info]]
            [wiki.web.routes :refer [app]]
            [wiki.data.versions :as version-data]
            [wiki.components.redis :as redisc]))

(defn- update-redirects [comp]
  (reset! (:redirects comp) (version-data/get-redirects (:jdbc comp))))

(defn- message-processor [comp]
  (fn [{:keys [message attempt]}]
    (info "Web message: " message)
    (when (= message "update-redirects")
      (update-redirects comp))
    {:status :success}))

(defn- component-middleware [component app]
  (fn [request]
    (app (assoc request :component component))))

(defn- create-web-app [component]
  (wrap-json-response
   (wrap-json-body
    (component-middleware component #'app)
    {:keywords? true})))

(defrecord Web [config web-server jdbc redis shpinx]
  component/Lifecycle

  (start [this]
    (let [comp (assoc this :redirects (atom (version-data/get-redirects jdbc)))]
      (assoc comp :web-server (server/run-server (create-web-app comp) config)
                  :engine (redisc/create-worker redis
                                               (-> this :config :redirects-queue)
                                               (message-processor comp)))))

  (stop [this]
    (if web-server
      (web-server :timeout 100))
    (redisc/delete-worker (:engine this))
    (assoc this :web-server nil
                :engine nil
                :redirects nil)))

(defn new-web [config]
  (map->Web {:config config}))
