(ns wiki.web.routes
  (:require [selmer.parser :refer [render-file]]
            [compojure.core :refer [defroutes routes GET POST]]
            [compojure.route :as route]
            [ring.util.response :refer [redirect response]]
            [ring.middleware.json :refer [wrap-json-response]]
            [wiki.components.redis :as redisca]
            [wiki.data.versions :as versions-data]
            [wiki.data.pages :as pages-data]))

(defn- jdbc [request]
  (-> request :component :jdbc))

(defn- redis [request]
  (-> request :component :redis))

(defn- notifier [request]
  (-> request :component :notifier))

(defn- request-update [request]
  (redisca/enqueue (redis request)
                   (-> request :component :config :queue)
                   "generate"))  

(defroutes app-routes
  (route/resources "/")
  (GET "/_update_" [] request-update)
  (POST "/_update_" [] request-update))

(def app (routes app-routes))
