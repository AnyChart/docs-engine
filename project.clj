(defproject wiki "1.1"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [compojure "1.1.6"]
                 [ring-server "0.3.0"]
                 [ring/ring-json "0.3.1"]
                 [selmer "0.5.9"]
                 [org.clojure/data.json "0.2.4"]
                 [markdown-clj "0.9.41"]
                 [com.taoensso/carmine "2.4.6"]
                 [sphinxapi "2.0.3"]
                 [http-kit "2.1.16"]]
  :main wiki.handlers
  :target-path "target/%s"
  :aot :all
  :profiles {:dev {:jvm-opts ["-Ddev=true"]}})
