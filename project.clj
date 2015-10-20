(defproject wiki "2.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [version-clj "0.1.0"]
                 ;; general
                 [com.stuartsierra/component "0.2.3"]
                 [clj-time "0.11.0"]
                 ;; templates
                 [selmer "0.8.2"]
                 ;; web
                 [http-kit "2.1.16"]
                 [compojure "1.1.9"]
                 [ring/ring-json "0.3.1"]
                 [ring/ring-core "1.3.1"]
                 ;; logging
                 [com.taoensso/timbre "4.0.1"]
                 ;; databases
                 [com.taoensso/carmine "2.11.1"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.2"]
                 [honeysql "0.5.2"]
                 ;; markdown
                 [markdown-clj "0.9.74"]]
  :main wiki.core
  :target-path "target/%s"
  :aot :all
  :profiles {:dev {:jvm-opts ["-Ddev=true"]}
             :uberjar {:jvm-opts []}})
