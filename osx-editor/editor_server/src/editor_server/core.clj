(ns editor_server.core
  (:use markdown.core)
  (:require [org.httpkit.server :refer [run-server]]
            [ring.util.response :refer [response]]
            [ring.middleware.params :refer [wrap-params]])
  (:import [java.net ServerSocket])
  (:gen-class :main :true))

(defn get-free-port!
  "Get a free port on the system."
  []
  (let [socket (ServerSocket. 0)
        port (.getLocalPort socket)]
    (.close socket)
    port))

(defn handler [request]
  (let [data ((request :form-params) "data")]
    (response (md-to-html-string data))))

(defn -main [& args]
  (let [port (get-free-port!)]
    (println port)
    (run-server (wrap-params handler {}) {:port port})))     
