(defproject wiki "2.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [version-clj "0.1.2"]
                 ;; general
                 [com.stuartsierra/component "0.3.2"]
                 [clj-time "0.14.4"]
                 [com.climate/claypoole "1.1.4"]
                 [toml "0.1.3"]
                 ;; templates
                 [selmer "1.11.8"]
                 [hiccup "1.0.5"]
                 ;; html
                 [enlive "1.1.6"]
                 [org.apache.commons/commons-lang3 "3.7"]
                 [com.anychart/playground-samples-parser "0.2.5"]
                 [com.anychart/link-checker "0.2.10"]
                 [org.jsoup/jsoup "1.11.3"]
                 [garden "1.3.5"]
                 ;; web
                 [http-kit "2.3.0"]
                 [compojure "1.6.1"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-core "1.6.3"]
                 [com.rpl/specter "1.1.1"]
                 ;; logging
                 [com.taoensso/timbre "4.10.0"]
                 ;; databases
                 [com.taoensso/carmine "2.18.1"]
                 ;; there are some problems with new jdbc using sphinx
                 [org.clojure/java.jdbc "0.7.7"]
                 [mysql/mysql-connector-java "5.1.36"]
                 [postgresql/postgresql "9.3-1102.jdbc41"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.3"]
                 [honeysql "0.9.3"]
                 ;; markdown
                 [markdown-clj "1.0.2"]
                 ;; filesystem
                 [me.raynes/fs "1.4.6"]
                 [cpath-clj "0.1.2"]
                 ;; phantom
                 [org.imgscalr/imgscalr-lib "4.2"]

                 [criterium "0.4.4"]]
  :plugins [[lein-ancient "0.6.10"]
            [lein-kibit "0.1.3"]]
  :main ^:aot wiki.core
  :target-path "target/%s"
  :aot :all
  :profiles {:dev     {:jvm-opts ["-Ddev=true"]}
             :uberjar {:jvm-opts []}})
