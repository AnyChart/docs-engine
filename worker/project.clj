(defproject worker "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.taoensso/carmine "2.4.6"]]
  :main ^:skip-aot worker.core
  :target-path "target/%s"
  :profiles {})
