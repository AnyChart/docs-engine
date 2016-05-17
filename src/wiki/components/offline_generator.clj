(ns wiki.components.offline-generator
  (:require [com.stuartsierra.component :as component]
            [wiki.offline.core :as offline]
            [wiki.components.redis :as redisc]
            [taoensso.timbre :as timbre :refer [info error]]))

(declare generate-zip)

(defn- message-processor [comp]
  (fn [{:keys [message attempt]}]
    (info "receive : " message)
    (when (= (:command message) "generate")
      (generate-zip comp (:version message)))
    {:status :success}))

(defrecord OfflineGenerator [config state jdbc redis]
  component/Lifecycle

  (start [this]
    (assoc this :engine (redisc/create-worker redis
                                              (-> this :config :queue)
                                              (message-processor this))))

  (stop [this]
    (redisc/delete-worker (:engine this))
    (dissoc this :engine)))

(defn new-offline-generator [config]
  (map->OfflineGenerator {:config config :state (atom {})}))

(defn- is-start-key [version]
  (str "is-start" (:key version)))

(defn- start-if-not-started [state config jdbc version]
  (let [status (get state (:key version))]
    (if (or (nil? status) (realized? status))
      (assoc state (:key version) (future
                                    (try
                                      (offline/generate-zip config jdbc version)
                                      (catch Exception e
                                        (error "Error generating zip " (:key version)
                                               "\n, error: " e
                                               "\n, message: " (.getMessage e)
                                               "\n, stacktrace: " (.printStackTrace e)
                                               "\n, cause stacktrace: " (.printStackTrace (.getCause e))
                                               "\n, cause message: " (.getMessage (.getCause e))))))
                   (is-start-key version) true)
      (assoc state (is-start-key version) false))))

(defn- generate-if-need [state config jdbc version]
  (swap! state start-if-not-started config jdbc version))

(defn generate-zip [offline-generator version]
  (let [state (:state offline-generator)
        config (:config offline-generator)
        jdbc (:jdbc offline-generator)
        new-state (generate-if-need state config jdbc version)
        is-start-generate (get new-state (is-start-key version))]
    (info "Is start generate for " version  ": " is-start-generate)
    is-start-generate))
