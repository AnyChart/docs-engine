(ns wiki.components.generator
  (:require [com.stuartsierra.component :as component]
            [wiki.components.redis :as redisc]
            [wiki.generator.core :as generator]))

(defn generate-docs [comp]
  (generator/generate (:jdbc comp)
                      (:notifier comp)
                      (:config comp)))

(defn- message-processor [comp]
  (fn [{:keys [message attempt]}]
    (when (= message "generate")
      (generate-docs comp)
      (redisc/enqueue (-> comp :redis)
                      (-> comp :config :indexer-queue)
                      "generate"))
    {:status :success}))

(defrecord Generator [config jdbc redis notifier]
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
