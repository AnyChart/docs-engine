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
            [wiki.config.core :as c]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [toml.core :as toml])
  (:gen-class))


(defn git-commit []
  (try
    (git/current-commit "/apps/keys/git" (.getAbsolutePath (clojure.java.io/file "")))
    (catch Exception _ (quot (System/currentTimeMillis) 1000))))


(defmacro parse-data-compile-time []
  `'~(git-commit))


(def commit (parse-data-compile-time))


(defn all-system [config]
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


(def config nil)


(defn update-config [conf]
  (-> conf
      (update-in [:generator :domain] keyword)
      (assoc-in [:web :commit] commit)))


(defn set-configs [config-path]
  (let [config-data (toml/read (slurp config-path) :keywordize)]
    (if (c/check-config config-data)
      (alter-var-root #'config (constantly (update-config (toml/read (slurp config-path) :keywordize))))
      (do
        (timbre/error (c/explain-config config-data))
        (System/exit 1)))))


(defn init-logger []
  ; Set the lowest-level to output as :debug
  (timbre/set-level! :debug)
  (Thread/setDefaultUncaughtExceptionHandler
    (reify Thread$UncaughtExceptionHandler
      (uncaughtException [_ thread ex]
        (timbre/error ex "Uncaught exception on" (.getName thread))))))


(def dev (all-system config))


(defn start []
  (alter-var-root #'dev component/start))


(defn stop []
  (alter-var-root #'dev component/stop))


(defn -main
  ([] (println "all|backend|frontend config-path"))
  ([mode config-path]
   (init-logger)
   (set-configs config-path)
   (cond
     (= mode "all") (component/start (all-system config))
     (= mode "frontend") (component/start (frontend-system config))
     (= mode "backend") (component/start (generator-system config))
     :else (timbre/info "Unknown domain or mode"))))