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
            [org.httpkit.client :as http]
            [cheshire.core :refer [generate-string]]
            [wiki.md :as md])
  (:gen-class :main :true))

(defn get-env-from-domain [request]
  (:server-name request))

(defn notify-slack []
  (http/post "https://anychart-team.slack.com/services/hooks/incoming-webhook?token=P8Z59E0kpaOqTcOxner4P5jb"
             {:form-params {:payload (generate-string {:text (str "Documentation deployed")
                                                       :channel "#notifications"
                                                       :username "docs-engine"})}}))

(defn rebuild [request]
  (versions/update)
  (notify-slack)
  (str "done"))

(defn redirect-version [request]
  (redirect (str "/" (-> request :route-params :version) "/Quick_Start")))

(defn check-document-middleware [app]
  (fn [request]
    (let [version (-> request :route-params :version)
          doc (-> request :route-params :*)]
      (if (docs/exists? version doc)
        (app request version doc)
        (route/not-found "Document not found")))))

(defn do-show-document [request version doc]
  (let [md-path (docs/md-path version doc)] 
    (render-file "templates/page.html" {:versions (versions/versions)
                                        :version version
                                        :groups (docs/grouped-documents version)
                                        :path doc
                                        :group (docs/get-group doc)
                                        :title (docs/title doc)
                                        :content (md/convert-markdown
                                                  version
                                                  (docs/get-content md-path)
                                                  (get-env-from-domain request))})))

(defn show-document [request]
  (let [version (-> request :route-params :version)
        path (-> request :route-params :*)]
    (if (docs/exists? version path)
      (do-show-document request version path)
      (let [group (clojure.string/replace path #"\/$" "")
            doc (docs/get-group-first-doc version group)]
        (if doc
          (redirect (str "/" version "/" doc))
          (route/not-found "Document not found"))))))

(defn show-document-json [request version doc]
  (let [md-path (docs/md-path version doc)]
    (response {:path doc
               :title (docs/title doc)
               :content (md/convert-markdown
                         version
                         (docs/get-content md-path)
                         (get-env-from-domain request))})))

(defroutes app-routes
  (route/resources "/")
  (GET "/" [] (redirect (str (versions/default) "/Quick_Start/Quick_Start")))
  (GET "/_pls_" [] rebuild)
  (POST "/_pls_" [] rebuild)
  (GET "/:version" [version] redirect-version)
  (GET "/:version/" [version] redirect-version)
  (GET "/:version/*-json" [version doc] (check-document-middleware show-document-json))
  (GET "/:version/*" [version doc] show-document)
  (route/not-found "Page not found"))

(def app
  (wrap-json-response (routes app-routes)))

(defn -main [& args]
  (println "starting server @9095")
  (server/run-server #'app {:port 9095}))
