(ns wiki.components.offline-generator
  (:require [com.stuartsierra.component :as component]
            [wiki.offline.core :as offline]
            [ring.util.response :refer [file-response header]]
            [taoensso.timbre :as timbre :refer [info error]]))

(defrecord OfflineGenerator [config state jdbc]
  component/Lifecycle

  (start [this] this)

  (stop [this] this))

(defn new-offline-generator [config]
  (map->OfflineGenerator {:config config :state (atom {})}))

(defn- is-start-key [version]
  (str "is-start" (:key version)))

(defn- start-if-not-started [state config jdbc version]
  (let [status (get state (:key version))]
    (if (or (nil? status) (realized? status))
      (assoc state (:key version) (future (offline/generate-zip config jdbc version))
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
    is-start-generate))

(defn download-zip [offline-generator version]
  (let [state (:state offline-generator)
        future-task (get @state (:key version))]
    (when (and (not (nil? future-task))
               (realized? future-task))
      @future-task)))