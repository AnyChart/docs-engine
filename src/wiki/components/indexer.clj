(ns wiki.components.indexer
  (:require [com.stuartsierra.component :as component]
            [wiki.components.redis :as redisc]
            [clojure.java.shell :refer [sh]]
            [taoensso.timbre :as timbre :refer [info error]]))

(defn- reindex-docs []
  (info (sh "/usr/bin/indexer" "--rotate" "--all")))

(defrecord Indexer [config redis]
  component/Lifecycle

  (start [this]
    (assoc this :engine (redisc/create-worker redis
                                              (-> this :config :queue)
                                              (fn [{:keys [message attempt]}]
                                                (if (= message "reindex")
                                                  (reindex-docs))
                                                {:status :success}))))

  (stop [this]
    (redisc/delete-worker (:engine this))
    (dissoc this :engine)))

(defn new-indexer [config]
  (map->Indexer {:config config}))
