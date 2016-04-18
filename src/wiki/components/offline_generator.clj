(ns wiki.components.offline-generator
  (:require [com.stuartsierra.component :as component]
            [wiki.offline.core :as offline]
            [ring.util.response :refer [file-response header]]))

(defrecord OfflineGenerator [config state jdbc]
  component/Lifecycle

  (start [this] this)

  (stop [this] this))

(defn new-offline-generator [config]
  (map->OfflineGenerator {:config config :state (atom {})}))

(defn- start-if-not-started [state jdbc version]
  (let [status (get state (:key version))]
    (if (nil? status)
      (assoc state (:key version) (future (offline/generate-zip jdbc version)))
      state)))

(defn- generate-if-need [state jdbc version]
  (swap! state start-if-not-started jdbc version))

(defn generate-zip [offline-generator version]
  (let [state (:state offline-generator)
        jdbc (:jdbc offline-generator)
        new-state (generate-if-need state jdbc version)
        future-task (get new-state (:key version))]
    (when (realized? future-task)
      @future-task)))

