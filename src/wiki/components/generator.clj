(ns wiki.components.generator
  (:require [com.stuartsierra.component :as component]
            [wiki.components.redis :as redisc]
            [wiki.generator.core :as generator]
            [taoensso.timbre :as timbre :refer [info error]]))

(defn generate-docs [comp]
  (generator/generate (:jdbc comp)
                      (:notifier comp)
                      (:offline-generator comp)
                      (:config comp)))

(defn- message-processor [comp]
  (fn [{:keys [message attempt]}]
    (when (= message "generate")
      (generate-docs comp)
      (info "enqueue in " (-> comp :config :indexer-queue) "with" (-> comp :redis))
      (redisc/enqueue (-> comp :redis)
                      (-> comp :config :indexer-queue)
                      "reindex"))
    {:status :success}))

(defrecord Generator [config jdbc redis notifier offline-generator]
  component/Lifecycle

  (start [this]
    (assoc this :engine (redisc/create-worker redis
                                              (-> this :config :queue)
                                              (message-processor this))))

  (stop [this]
    (redisc/delete-worker (:engine this))
    (dissoc this :engine)))

(defn new-generator [config]
  (map->Generator {:config config}))
