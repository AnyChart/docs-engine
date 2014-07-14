(ns editor_server.core
  (:require [markdown.core :refer :all]
            [compojure.core :refer [defroutes routes GET POST]]
            [markdown.transformers :refer :all]
            [clojure.contrib.str-utils2 :as str-utils]
            [org.httpkit.server :refer [run-server]]
            [ring.util.response :refer [response]]
            [ring.middleware.params :refer [wrap-params]])
  (:import [java.net ServerSocket])
  (:gen-class :main :true))

(defn escape-sample-path [sample-path] sample-path)

(defn build-sample-embed [sample-path]
  (str
   "<div class='sample'>
     <p>Live sample</p>
     <a target='_blank' href='//playground.anychart.com/acdvf-docs/{{VERSION}}/samples/" sample-path "'>Launch in playground</a>
     <iframe src='{{SAMPLES_BASE}}/sample?path=" (escape-sample-path sample-path) ".html&base={{BASE}}'></iframe></div>"))

(defn sample-transformer [text state]
  [(if (or (:code state) (:codeblock state))
     text
     (let [matches (re-matches #".*(\{sample\}(.*)\{sample\}).*" text)
           sample-path (nth matches 2)
           source (nth matches 1)]
       (if sample-path
         (str-utils/replace text source (build-sample-embed sample-path))
         text)))
   state])

(defn get-free-port!
  "Get a free port on the system."
  []
  (let [socket (ServerSocket. 0)
        port (.getLocalPort socket)]
    (.close socket)
    port))

(defn handler [request]
  (let [data ((request :form-params) "data")]
    (response (md-to-html-string data
                                 :heading-anchors true
                                 :custom-transformers [sample-transformer]))))

(defn sample-handler [request]
  (let [data ((request :params) "path")
        base-path ((request :params) "base")]
    (response (str "<!doctype html>
<html>
  <head>
    <script src='/js?f=anychart.min.js&base=" base-path "'></script>
    <style type='text/css'>
      html, body, #container { width: 100%; height: 100%; margin: 0; padding: 0; overflow: hidden; }
    </style>
  </head>
  <body>
    <div id='container'></div>
    <script>" (clojure.string/replace (slurp (str base-path "/samples/" data)) #"<sample>|</sample>" "") "</script></body></html>"))))

(defn js-handler [request]
  (let [f ((request :params) "f")
        base-path ((request :params) "base")]
    (response (slurp (str base-path "/js/" f)))))

(defroutes app-routes
  (POST "/" [] handler)
  (GET "/js" [] js-handler)
  (GET "/sample" [] sample-handler))

(defn -main [& args]
  (let [port (get-free-port!)]
    (println port)
    (run-server (wrap-params app-routes {}) {:port port})))     
