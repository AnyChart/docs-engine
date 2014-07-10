(ns wiki.repl
  (:require [org.httpkit.server :as server]
            [wiki.handlers :refer [app]]))

(defonce server (atom nil))

(defn stop []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start []
  (stop)
  (reset! server (server/run-server #'app {:port 9090})))

(defn restart []
  (stop)
  (start))
