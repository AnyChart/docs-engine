(defproject wiki "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [ring-server "0.3.0"]
                 [selmer "0.5.9"]
                 [markdown-clj "0.9.41"]
                 [http-kit "2.1.16"]]
  :plugins [[lein-ring "0.8.7"]]
  :ring {:handler wiki.handler/app
         :init wiki.handler/init
         :destroy wiki.handler/destroy}
  :aot :all
  :profiles
  {:production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev
   {:dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.2.0"]]}})
