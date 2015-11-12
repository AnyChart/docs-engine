(ns wiki.components.sphinx
  (:require [clojure.java.jdbc :as clj-jdbc]
            [com.stuartsierra.component :as component]))

(defrecord Sphinx [config conn]
  component/Lifecycle
  (start [this]
    this)
  (stop [this]
    this))

(defn new-sphinx [config]
  (map->Sphinx {:config config}))

(defn query [jdbc query params]
  (clj-jdbc/query (:config jdbc) (concat [query] params)))
