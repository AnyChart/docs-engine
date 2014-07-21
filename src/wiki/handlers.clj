(ns wiki.handlers
  (:require [selmer.parser :refer [render-file]]
            [compojure.core :refer [defroutes routes GET POST]]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [org.httpkit.server :as server]
            [wiki.versions :as versions]
            [wiki.documents :as docs]
            [ring.util.response :refer [response]]
            [ring.middleware.json :refer [wrap-json-response]]
            [wiki.md :as md])
  (:gen-class :main :true))

(defn get-env-from-domain [request]
  (:server-name request))

(defn rebuild [request]
  (versions/update)
  "Ты пришел и говоришь: движок, мне нужна документация. Но ты просишь без уважения, ты не предлагаешь дружбу, ты даже не сделал мне pull request.<br />Тем не менее я выполню твою просьбу.")

(defn redirect-version [request]
  (redirect (str "/" (-> request :route-params :version) "/Quick_Start")))

(defn check-document-middleware [app]
  (fn [request]
    (let [version (-> request :route-params :version)
          doc (-> request :route-params :*)]
      (if (docs/exists? version doc)
        (app request version doc)
        (route/not-found "Document not found")))))

(defn show-document [request version doc]
  (println request)
  (let [md-path (docs/md-path version doc)] 
    (render-file "templates/page.html" {:versions (versions/versions)
                                        :version version
                                        :groups (docs/grouped-documents version)
                                        :path doc
                                        :title (docs/title doc)
                                        :content (md/convert-markdown
                                                  version
                                                  md-path
                                                  (get-env-from-domain request))})))

(defn show-document-json [request version doc]
  (let [md-path (docs/md-path version doc)]
    (response {:path doc
               :title (docs/title doc)
               :content (md/convert-markdown
                         version
                         md-path
                         (get-env-from-domain request))})))

(defroutes app-routes
  (route/resources "/")
  (GET "/" [] (redirect (str (versions/default) "/Quick_Start")))
  (GET "/_pls_" [] rebuild)
  (POST "/_pls_" [] rebuild)
  (GET "/:version" [version] redirect-version)
  (GET "/:version/" [version] redirect-version)
  (GET "/:version/*-json" [version doc] (check-document-middleware show-document-json))
  (GET "/:version/*" [version doc] (check-document-middleware show-document))
  (route/not-found "Page not found"))

(def app
  (wrap-json-response (routes app-routes)))

(defn -main [& args]
  (println "starting server @9095")
  (server/run-server #'app {:port 9095}))
