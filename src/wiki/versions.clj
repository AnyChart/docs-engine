(ns wiki.versions
  (:require [wiki.config :as config]
            [wiki.git :as git]
            [taoensso.carmine :as car]
            [wiki.data :refer (wcar*)]
            [version-clj.core :refer [version-compare]]
            [wiki.documents :as docs]))

(def redis-versions-key
  (str "docs_versions"))

(defn versions [])

(defn exists? [version])

(defn default [])

(defn clean [])

(defn update [])
