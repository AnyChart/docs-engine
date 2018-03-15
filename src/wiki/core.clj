(ns wiki.core
  (:require [wiki.components.jdbc :as jdbc]
            [wiki.components.redis :as redis]
            [wiki.components.notifier :as notifier]
            [wiki.components.generator :as generator]
            [wiki.components.sphinx :as sphinx]
            [wiki.components.indexer :as indexer]
            [wiki.components.web :as web]
            [wiki.generator.git :as git]
            [wiki.components.offline-generator :as offline-generator]
            [wiki.util.utils :as utils]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [toml.core :as toml])
  (:gen-class :main :true))


(defn git-commit []
  (try
    (git/current-commit "/apps/keys/git" (.getAbsolutePath (clojure.java.io/file "")))
    (catch Exception _ (quot (System/currentTimeMillis) 1000))))


(defmacro parse-data-compile-time []
  `'~(git-commit))


(def commit (parse-data-compile-time))


(defn dev-system [config]
  (component/system-map
    :notifier (notifier/new-notifier (:notifications config))
    :jdbc (jdbc/new-jdbc (:jdbc config))
    :redis (redis/new-redis (:redis config))
    :sphinx (sphinx/new-sphinx (:sphinx config))
    :web (component/using (web/new-web (:web config))
                          [:jdbc :redis :notifier :sphinx :offline-generator])
    :generator (component/using (generator/new-generator (:generator config))
                                [:jdbc :redis :notifier :offline-generator])
    :offline-generator (component/using (offline-generator/new-offline-generator (:offline-generator config))
                                        [:jdbc :redis])
    :indexer (component/using (indexer/new-indexer (:indexer config))
                              [:redis])))


(defn frontend-system [config]
  (component/system-map
    :notifier (notifier/new-notifier (:notifications config))
    :jdbc (jdbc/new-jdbc (:jdbc config))
    :redis (redis/new-redis (:redis config))
    :sphinx (sphinx/new-sphinx (:sphinx config))
    :indexer (component/using (indexer/new-indexer (:indexer config))
                              [:redis])
    :web (component/using (web/new-web (:web config))
                          [:jdbc :redis :notifier :sphinx])))


(defn generator-system [config]
  (component/system-map
    :notifier (notifier/new-notifier (:notifications config))
    :jdbc (jdbc/new-jdbc (:jdbc config))
    :redis (redis/new-redis (:redis config))
    :offline-generator (component/using (offline-generator/new-offline-generator (:offline-generator config))
                                        [:jdbc :redis])
    :generator (component/using (generator/new-generator (:generator config))
                                [:jdbc :redis :notifier :offline-generator])))


(def all-config nil)

(def config nil)

(def stg-config nil)

(def prod-config)


(defn update-config [conf]
  (-> conf
      (update-in [:generator :domain] keyword)
      (assoc-in [:web :commit] commit)))


(defn set-configs [config-path]
  (alter-var-root #'all-config (constantly (toml/read (slurp config-path) :keywordize)))
  (alter-var-root #'config (constantly (-> all-config :base update-config)))
  (alter-var-root #'stg-config (constantly (update-config (utils/deep-merge config (:stg all-config)))))
  (alter-var-root #'prod-config (constantly (update-config (utils/deep-merge config (:prod all-config))))))


(defn init-logger []
  ; Set the lowest-level to output as :debug
  (timbre/set-level! :debug)
  (Thread/setDefaultUncaughtExceptionHandler
    (reify Thread$UncaughtExceptionHandler
      (uncaughtException [_ thread ex]
        (timbre/error ex "Uncaught exception on" (.getName thread))))))


(def dev (dev-system config))


(defn start []
  (alter-var-root #'dev component/start))


(defn stop []
  (alter-var-root #'dev component/stop))


(defn -main
  ([] (println "dev backend|stg frontend|stg backend|com frontend|com config-path"))
  ([mode config-path]
   (init-logger)
   (set-configs config-path)
   (if (= mode "dev")
     (component/start (dev-system config))
     (timbre/warn "Unknown mode")))
  ([domain mode config-path]
   (init-logger)
   (set-configs config-path)
   (cond
     (and (= domain "stg") (= mode "frontend")) (component/start (frontend-system stg-config))
     (and (= domain "stg") (= mode "backend")) (component/start (generator-system stg-config))
     (and (= domain "prod") (= mode "frontend")) (component/start (frontend-system prod-config))
     (and (= domain "prod") (= mode "backend")) (component/start (generator-system prod-config))
     :else (timbre/info "Unknown domain or mode"))))