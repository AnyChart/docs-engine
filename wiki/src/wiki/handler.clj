(ns wiki.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [compojure.route :as route]
            [clojure.java.io :refer [file]]
            [selmer.parser :refer [render-file]]
            [selmer.filters :refer [add-filter!]]
            [markdown.core :refer [md-to-html-string]]
            [taoensso.carmine :as car :refer (wcar)]
            [clojure.edn :as edn]))

(def config (:app (edn/read-string (slurp "/app/config.clj"))))
(def data-path (str (:data config) "/data/"))
(selmer.parser/set-resource-path! "/app/wiki/src/templates/")

(add-filter! :safe (fn [x] [:safe x]))

(def env (System/getenv))
(def redis-conn {:pool {} :spec {:host (get env "REDIS_PORT_6379_TCP_ADDR")
                                 :port (read-string (get env "REDIS_PORT_6379_TCP_PORT"))}})
(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

(defn get-versions []
  (sort (map (fn [f] (.getName f))
             (filter (fn [f] (.isDirectory f))
                     (.listFiles (file data-path))))))

(defn get-pages [version]
  (tree-seq
     (fn [f] (and (.isDirectory f) (not (.isHidden f))))
     (fn [d] (filter (fn [f] (and (not (.isHidden f)))) (.listFiles d)))
     (file (str data-path version))))

(defn get-wiki-pages [version]
  (map (fn[f]
         (let [relative-path
               (clojure.string/replace (.getAbsolutePath f)
                                       (clojure.string/re-quote-replacement
                                        (str data-path version "/"))
                                       "")]
                {:file f
                 :name (clojure.string/replace relative-path #"\.md$" "")}))
       (filter (fn [f] (.endsWith (.getName f) ".md"))
               (get-pages version))))

(defn page-exists? [path]
  (.exists (file (str data-path path ".md"))))

(defn render-page [path version page raw-page]
  (render-file "page.html" {:versions (get-versions)
                            :version version
                            :path page
                            :raw-path raw-page
                            :pages (get-wiki-pages version)
                            :content (md-to-html-string (slurp (str data-path path ".md")))}))

(defn check-page [request]
  (let [params (:route-params request)
        version (:version params)
        path (:* params)]
    (if (page-exists? (str version "/" path))
      (render-page (str version "/" path) version path (str path ".md"))
      (if (page-exists? (str version "/" path "/index"))
        (render-page (str version "/" path "/index") version path "index.md")))))

(defn check-version-page [request]
  (let [version (:version (:route-params request))]
    (if (page-exists? (str version "/index"))
      (render-page (str version "/index") version "" "index.md"))))

(defn send-rebuild-signal [request]
  (wcar* (car/publish "docs" "rebuild"))
  (str "rebuilding..."))

(defroutes app-routes
  (GET "/_pls_/" [] send-rebuild-signal)
  (GET "/:version/*/" [version path] check-page)
  (GET "/:version/" [version] check-version-page)
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)))


