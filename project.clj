(defproject wiki "2.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [version-clj "0.1.2"]
                 ;; general
                 [com.stuartsierra/component "0.3.2"]
                 [clj-time "0.13.0"]
                 [com.climate/claypoole "1.1.4"]
                 [toml "0.1.2"]
                 ;; templates
                 [selmer "1.10.6"]
                 [hiccup "1.0.5"]
                 ;; html
                 [enlive "1.1.6"]
                 [org.apache.commons/commons-lang3 "3.5"]
                 [com.anychart/playground-samples-parser "0.2.1"]
                 [com.anychart/link-checker "0.2.6"]
                 [org.jsoup/jsoup "1.10.2"]
                 ;; web
                 [http-kit "2.2.0"]
                 [compojure "1.5.2"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-core "1.5.1"]
                 ;; logging
                 [com.taoensso/timbre "4.8.0"]
                 ;; databases
                 [com.taoensso/carmine "2.15.1"]
                 ;; there are some problems with new jdbc using sphinx
                 [org.clojure/java.jdbc "0.3.6"]
                 [mysql/mysql-connector-java "5.1.36"]
                 [postgresql/postgresql "9.3-1102.jdbc41"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.2"]
                 [honeysql "0.8.2"]
                 ;; markdown
                 [markdown-clj "0.9.99"]
                 ;; filesystem
                 [me.raynes/fs "1.4.6"]
                 [cpath-clj "0.1.2"]
                 ;; phantom
                 [org.imgscalr/imgscalr-lib "4.2"]

                 [criterium "0.4.4"]
                 ;; optimizations
                 [com.googlecode.htmlcompressor/htmlcompressor "1.5.2"]]
  :plugins [[lein-ancient "0.6.10"]
            [lein-kibit "0.1.3"]]
  :main wiki.core
  :target-path "target/%s"
  :aot :all
  :profiles {:dev {:jvm-opts ["-Ddev=true"]}
             :uberjar {:jvm-opts []}})
