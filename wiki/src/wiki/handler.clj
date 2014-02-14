(ns wiki.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [compojure.route :as route]
            [clojure.java.io :refer [file]]
            [selmer.parser :refer [render-file]]
            [selmer.filters :refer [add-filter!]]
            [markdown.core :refer [md-to-html-string]]
            [clojure.edn :as edn]))

(def config (:app (edn/read-string (slurp "/app/config.clj"))))
(def data-path (str (:data config) "/data/"))
(selmer.parser/set-resource-path! "/app/wiki/src/templates/")

(add-filter! :safe (fn [x] [:safe x]))

(defn get-versions []
  (sort (map (fn [f] (.getName f))
             (filter (fn [f] (.isDirectory f))
                     (.listFiles (file data-path))))))

(defn page-exists? [path]
  (.exists (file (str data-path path ".md"))))

(defn render-page [path]
  (render-file "page.html" {:versions (get-versions)
                            :content (md-to-html-string (slurp (str data-path path ".md")))}))

(defn check-page [request]
  (let [params (:route-params request)
        version (:version params)
        path (:* params)]
    (if (page-exists? (str version "/" path))
      (render-page (str version "/" path))
      (if (page-exists? (str version "/" path "/index"))
        (render-page (str version "/" path "/index"))
        (str "page not found :( '" path "'")))))

(defn check-version-page [request]
  (let [version (:version (:route-params request))]
    (if (page-exists? (str version "/index"))
      (render-page (str version "/index")))))

(defn index [request]
  (str "test index"))

(defn send-rebuild-signal [request]
  (str "rebuilding..."))

(defroutes app-routes
  (GET "/" [] index)
  (GET "/^_^" [] send-rebuild-signal)
  (GET "/:version/*/" [version path] check-page)
  (GET "/:version/" [version] check-version-page)
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)))


