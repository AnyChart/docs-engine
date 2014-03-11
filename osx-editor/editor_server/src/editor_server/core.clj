(ns editor_server.core
  (:use markdown.core)
  (:gen-class :main :true))

(defn convert-file [src out]
  (md-to-html src out))

(defn -main [& args]
   (convert-file (first args) (nth args 1)))
