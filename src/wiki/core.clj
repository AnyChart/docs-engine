(ns wiki.core
  (:require [wiki.components.jdbc :as jdbc]
            [wiki.components.redis :as redis]
            [wiki.components.notifier :as notifier]
            [wiki.components.generator :as generator]
            [wiki.components.sphinx :as sphinx]
            [wiki.components.indexer :as indexer]
            [wiki.components.web :as web]
            [wiki.components.offline-generator :as offline-generator]
            [com.stuartsierra.component :as component])
  (:gen-class :main :true))

(defn dev-system [config]
  (component/system-map
   :notifier (notifier/new-notifier (:notifications config))
   :jdbc  (jdbc/new-jdbc (:jdbc config))
   :redis (redis/new-redis (:redis config))
   :sphinx (sphinx/new-sphinx (:sphinx config))
   :web   (component/using (web/new-web (:web config))
                           [:jdbc :redis :notifier :sphinx :offline-generator])
   :generator (component/using (generator/new-generator (:generator config))
                               [:jdbc :redis :notifier])
   :offline-generator (component/using (offline-generator/new-offline-generator {})
                                       [:jdbc])
   :indexer (component/using (indexer/new-indexer (:indexer config))
                             [:redis])))

(defn frontend-system [config]
  (component/system-map
   :notifier (notifier/new-notifier (:notifications config))
   :jdbc  (jdbc/new-jdbc (:jdbc config))
   :redis (redis/new-redis (:redis config))
   :sphinx (sphinx/new-sphinx (:sphinx config))
   :indexer (component/using (indexer/new-indexer (:indexer config))
                             [:redis])
   :offline-generator (component/using (offline-generator/new-offline-generator {})
                                       [:jdbc])
   :web   (component/using (web/new-web (:web config))
                           [:jdbc :redis :notifier :sphinx :offline-generator])))

(defn generator-system [config]
  (component/system-map
   :notifier (notifier/new-notifier (:notifications config))
   :jdbc  (jdbc/new-jdbc (:jdbc config))
   :redis (redis/new-redis (:redis config))
   :generator (component/using (generator/new-generator (:generator config))
                               [:jdbc :redis :notifier])))

(def base-config
  {:notifications {:token "P8Z59E0kpaOqTcOxner4P5jb"
                   :channel "#notifications-local"
                   :username "docs-engine"
                   :domain "http://localhost/"}
   :indexer {:queue "docs-stg-search-queue"}
   :web {:debug true
         :static 12
         :port 8080
         :queue "docs-queue"
         :reference "api.anychart.stg"
         :playground "playground.anychart.stg"}
   :jdbc {:subprotocol "postgresql"
          :subname "//localhost:5432/docs_db"
          :classname "org.postgresql.Driver"
          :user "docs_user"
          :password "pass"}
   :sphinx {:subprotocol "mysql"
            :subname "//104.236.66.244:3312?characterEncoding=utf8&characterSetResults=utf8&maxAllowedPacket=512000"
            :table "docs_stg_index"}
   :redis {:pool {}
           :spec {:host "127.0.0.1" :port 6379 :db 0}}
   :generator {:show-branches true
               :git-ssh "/Users/alex/Work/anychart/reference-engine/keys/git"
               :data-dir (.getAbsolutePath (clojure.java.io/file "data"))
               :max-processes 8
               :queue "docs-queue"
               :indexer-queue "docs-stg-search-queue"
               :reference "api.anychart.stg"
               :reference-versions "http://api.anychart.stg/versions"
               :reference-default-version "develop"
               :playground "playground.anychart.stg/docs"}})

(def config base-config)

(def stg-config (merge-with merge base-config
                            {:notifications {:domain "http://docs.anychart.stg/" :channel "#notifications-stg"}}
                            {:web {:debug false
                                   :port 9010}}
                            {:jdbc {:subname "//10.132.9.26:5432/docs_stg"
                                    :user "docs_stg_user"
                                    :password "fuckstg"}}
                            {:redis {:spec {:host "10.132.9.26" :db 1}}}
                            {:generator {:git-ssh "/apps/keys/git"
                                         :data-dir "/apps/docs-stg/data"}}))

(def prod-config (merge-with merge base-config
                             {:notifications {:domain "https://docs.anychart.com/" :channel "#notifications-prod"}}
                             {:web {:debug false
                                    :port 9011
                                    :queue "docs-prod-queue"
                                    :reference "api.anychart.com"
                                    :playground "playground.anychart.com"}}
                             {:jdbc {:subname "//10.132.9.26:5432/docs_prod"
                                     :user "docs_prod_user"
                                     :password "fuckprod"}}
                             {:redis {:spec {:host "10.132.9.26" :db 1}}}
                             {:sphinx {:table "docs_prod_index"}}
                             {:generator {:show-branches false
                                          :git-ssh "/apps/keys/git"
                                          :data-dir "/apps/docs-prod/data"
                                          :queue "docs-prod-queue"
                                          :indexer-queue "docs-prod-search-queue"
                                          :reference "api.anychart.com"
                                          :reference-versions "https://api.anychart.com/versions"
                                          :reference-default-version "latest"
                                          :playground "playground.anychart.com/docs"}}))

(def dev (dev-system config))

(defn start []
  (alter-var-root #'dev component/start))

(defn stop []
  (alter-var-root #'dev component/stop))

(defn -main
  ([] (println "dev keys-path|stg frontend|stg backend|com frontend|com backend ??"))
  ([mode]
   (if (= mode "dev")
     (component/start (dev-system config))
     (println "started at http://localhost:8010")))
  ([domain mode]
   (cond
     (= domain "dev") (component/start (dev-system (assoc-in config [:generator :git-ssh] mode)))
     (and (= domain "stg") (= mode "frontend")) (component/start (frontend-system stg-config))
     (and (= domain "stg") (= mode "backend")) (component/start (generator-system stg-config))
     (and (= domain "prod") (= mode "frontend")) (component/start (frontend-system prod-config))
     (and (= domain "prod") (= mode "backend")) (component/start (generator-system prod-config)))))
