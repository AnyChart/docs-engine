(ns indexer.core
  (:require [clojure.java.io :refer [file]]
            [clojure.data.xml :as xml]
            [clojure.java.shell :refer [sh]]
            [taoensso.carmine :as car :refer (wcar)])
  (:gen-class :main :true))

(def redis-conn {:pool {} :spec {:host (System/getenv "REDIS_PORT_6379_TCP_ADDR")
                                 :port (Integer/parseInt
                                        (System/getenv "REDIS_PORT_6379_TCP_PORT"))}})

(def config {:data "/wiki"})
(def data-path (str (:data config) "/data/"))

(def id (atom 0))

(defn get-pages [version]
  (filter (fn [f] (= "md" (last (.split (.getName f) "\\."))))
          (tree-seq
           (fn [f] (and (.isDirectory f) (not (.isHidden f))))
           (fn [d] (filter (fn [f]
                             (not (.isHidden f)))
                           (.listFiles d)))
           (file (str data-path version)))))

(defn file-xml [f]
  (swap! id inc)
  (xml/element "sphinx:document" {:id @id}
               (xml/element :name {} (.getPath f))
               (xml/element :published {} (str (.lastModified f)))
               (xml/element :content {} (xml/cdata (slurp (.getPath f))))))

(defn get-xml [version]
  (xml/element "sphinx:docset" { "xmlns:sphinx" "http://sphinxsearch.com/"}
               (xml/element "sphinx:schema" {}
                            (xml/element "sphinx:attr" {:name "name" :type "string"})
                            (xml/element "sphinx:field" {:name "content"}))
               (reduce (fn [val f] (conj val (file-xml f)))
                       () (get-pages version))))

(defn get-versions []
   (filter (fn [f] (.isDirectory f))
                  (.listFiles (file data-path))))

(defn get-sphinx-indexes []
  (reduce (fn [config el]
            (let [name (.getName el)
                  key (clojure.string/escape name {\. "_"})]
              (str config "source " key " {
  type = xmlpipe2
  xmlpipe_command = java -jar /apps/wiki/indexer.jar " name "
}

index " key "_index {
  source = " key "
  path = /sphinx/indexes/" key "_index
  morphology = stem_en
  charset_type = utf-8
  min_word_len = 3
  html_strip = 1
}
")))       
          ""
          (get-versions)))

(defn get-sphinx-config []
  (str (get-sphinx-indexes)
       "indexer
{
  mem_limit = 256M
  max_iops = 0
  max_xmlpipe2_field = 100M
}

searchd
{
  pid_file = /sphinx/searchd.pid
  log = /sphinx/log/sphinx/searchd.log
  query_log = /sphinx/log/sphinx/searchd_query.log
  listen = 0.0.0.0:49005
}"))

(defn rebuild-indexes []
  (println "rebuilding indexes")
  (spit "/etc/sphinxsearch/sphinx.conf" (get-sphinx-config))
  (sh "/usr/bin/indexer" "--config" "/etc/sphinxsearch/sphinx.conf" "--rotate" "--all")
  (sh "chown" "-R" "sphinxsearch:sphinxsearch" "/sphinx")
  (sh "service" "sphinxsearch" "restart"))

(defn start-server []
  (println "start server")
  (spit "/etc/default/sphinxsearch" "START=yes")
  (car/with-new-pubsub-listener (:spec redis-conn)
    {"reindex" (fn [msg] (println (rebuild-indexes)))}
    (car/subscribe "reindex"))
  (sh "service" "sphinxsearch" "restart"))

(defn -main [& args]
  (if (= "config" (first args))
    (println (get-sphinx-config))
    (if (= "server" (first args))
      (start-server)
      (println (xml/emit-str (get-xml (first args)))))))
