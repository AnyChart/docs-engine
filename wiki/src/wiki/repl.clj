(ns wiki.repl
  (:require [org.httpkit.server :as server]
            [wiki.handler :as handler]))

(defonce server (atom nil))

(defn stop []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start []
  (stop)
  (reset! server (server/run-server #'handler/app {:port 9090})))

(defn restart []
  (stop)
  (start))
