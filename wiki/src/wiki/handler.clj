(ns wiki.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [compojure.route :as route]
            [clojure.java.io :refer [file]]
            [selmer.parser :refer [render-file]]
            [selmer.filters :refer [add-filter!]]
            [markdown.core :refer [md-to-html-string]]
            [org.httpkit.server :as server]
            [ring.util.codec :refer [form-decode]]
            [clojure.walk :refer [keywordize-keys]]
            [taoensso.carmine :as car :refer (wcar)])
  (:gen-class :main :true))

(def config {:data "/wiki"})
(def data-path (str (:data config) "/data/"))
(selmer.parser/set-resource-path! "/apps/wiki/templates/")

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
  (wcar* (car/publish "reindex" "reindex"))
  (str "rebuilding..."))

(defn process-search-results [sphinx-client docs words version-key version]
  (map (fn [doc] {:path (clojure.string/replace
                         (clojure.string/replace
                          (clojure.string/replace doc #"/wiki/data" "")
                          #"\.index.md$" "")
                         #"\.md$" "")
                  :results (seq (.BuildExcerpts
                                  sphinx-client
                                  (into-array String [(md-to-html-string (slurp doc))])
                                  version-key
                                  (clojure.string/join words)
                                  (java.util.HashMap. { })))}) docs))
  
(defn search-for [q version-key version]
  (if (not (clojure.string/blank? q))

    (let [sphinx-client (org.sphx.api.SphinxClient.
                    (System/getenv "INDEXER_PORT_49005_TCP_ADDR")
                    (read-string (System/getenv "INDEXER_PORT_49005_TCP_PORT")))]
      (.Open sphinx-client)
      (.SetMatchMode sphinx-client 2)
      (let [res (.Query sphinx-client q version-key)]
        (println "query result" res)
        (prn res)
        (prn (seq (.matches res)))
        (let [
              docs (map #(first (.attrValues %)) (seq (.matches res)))
              words (map #(.word %) (seq (.words res)))]
          (process-search-results sphinx-client docs words version-key version))))))

(defn search-request [request]
  (let [version (:version (:route-params request))
        version-key (str (clojure.string/escape version {\. "_"}) "_index")
        q (if (:query-string request)
            (:q (keywordize-keys (form-decode (:query-string request))))
            "")
        search-results (search-for q version-key version)]
    (render-file "search-results.html" {:versions (get-versions)
                                        :version version
                                        :results search-results
                                        :q q})))

(defroutes app-routes
  (GET "/_pls_" [] send-rebuild-signal)
  (GET "/:version/search" [version] search-request)
  (GET "/:version/*" [version path] check-page)
  (GET "/:version" [version] check-version-page)
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)))

(defn -main [& args]
  (server/run-server #'app {:port 9090}))

