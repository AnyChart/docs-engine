(ns editor_server.core
  (:require [markdown.core :refer :all]
            [markdown.transformers :refer :all]
            [clojure.contrib.str-utils2 :as str-utils]
            [org.httpkit.server :refer [run-server]]
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

(defn sample-transformer [text state]
  [(if (or (:code state) (:codeblock state))
     text
     (let [matches (re-matches #".*(\{sample\}(.*)\{sample\}).*" text)
           sample-path (nth matches 2)
           source (nth matches 1)]
       (if sample-path
         (str-utils/replace text source "zz")
         text)))
   state])

(defn handler [request]
  (let [data ((request :form-params) "data")]
    (response (md-to-html-string data
                                 :heading-anchors true
                                 :custom-transformers [sample-transformer]))))

(defn -main [& args]
  (let [port (get-free-port!)]
    (println port)
    (run-server (wrap-params handler {}) {:port port})))     
