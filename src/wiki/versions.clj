(ns wiki.versions
  (:require [clojure.tools.logging :as log]
            [wiki.config :as config]
            [wiki.git :as git]
            [taoensso.carmine :as car]
            [wiki.data :refer (wcar*)]
            [wiki.documents :as docs]))

(def redis-versions-key
  (str "docs_versions"))

(defn versions []
  (wcar* (car/smembers redis-versions-key)))

(defn exists? [version]
  (= 1 (wcar* (car/sismember redis-versions-key version))))

(defn default []
  (first (versions)))

(defn clean []
  (log/info "cleanup redis")
  (docs/clean (versions))
  (wcar* (car/del redis-versions-key)))

(defn update []
  (log/info "updating versions")
  (clean)
  (git/update config/repo-path)
  (log/info "include branches?" (:show-branches config/config))
  (git/lock config/repo-path)
  (git/run-sh "rm" "-rf" (str config/versions-path))
  (git/run-sh "mkdir" (str config/versions-path))
  (let [versions (if (:show-branches config/config)
                   (git/actual-branches config/repo-path)
                   (git/version-branches config/repo-path))]
    (log/info "available versions:" versions)
    (doseq [v versions]
      (log/info "generating" v)
      (wcar* (car/sadd redis-versions-key v))
      (git/checkout-to config/repo-path
                       v
                       (str config/versions-path "/" v))
      (docs/update v)))
  (git/unlock config/repo-path)
  (log/info "versions updated"))
