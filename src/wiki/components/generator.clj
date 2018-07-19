(ns wiki.components.generator
  (:require [com.stuartsierra.component :as component]
            [wiki.components.redis :as redisc]
            [wiki.generator.core :as generator]
            [taoensso.timbre :as timbre :refer [info error]]))


(defn generate-docs [comp gen-params]
  (generator/generate comp
                      (swap! (:queue-index (:config comp)) inc)
                      gen-params)
  (redisc/enqueue (:redis comp)
                  (-> comp :config :redirects-queue)
                  "update-redirects")
  (System/gc))


(defn message-processor [comp]
  (fn [{:keys [message attempt]}]
    (let [{:keys [cmd]} message]
      (when (= cmd "generate")
        (generate-docs comp message)
        (info "enqueue in " (-> comp :config :indexer-queue) "with" (-> comp :redis))
        (redisc/enqueue (-> comp :redis)
                        (-> comp :config :indexer-queue)
                        "reindex")))
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
  (map->Generator {:config (assoc config :queue-index (atom 0))}))
