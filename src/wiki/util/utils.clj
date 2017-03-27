(ns wiki.util.utils
  (:require [clojure.string :as s])
  (import [org.jsoup Jsoup]))

(defn name->url [name]
  (-> name
      (clojure.string/replace #"^/" "")
      (clojure.string/replace #"/" "-")
      (clojure.string/replace #", " "-")
      (clojure.string/replace #",_" "-")
      (clojure.string/replace #"," "-")
      (clojure.string/replace #" " "-")
      (clojure.string/replace #"_" "-")
      s/lower-case))

(defn released-version? [version-key]
  (re-matches #"^\d+\.\d+\.\d+$" version-key))

(defn anychart-bundle-path [version-key]
  (if (released-version? version-key)
    (str "https://cdn.anychart.com/js/" version-key "/anychart-bundle.min.js")
    (str "http://static.anychart.com/js/" version-key "/anychart-bundle.min.js")))

(defn anychart-bundle-css-url [version-key]
  (if (released-version? version-key)
    (str "https://cdn.anychart.com/css/" version-key "/anychart-ui.min.css")
    (str "http://static.anychart.com/css/" version-key "/anychart-ui.min.css")))

(defn url->title [url]
  (let [parts (-> url
                (s/replace #"_" " ")
                (s/split #"/")
                reverse)]
    (s/join " | " parts )))

(defn page-description [html]
  (let [doc (Jsoup/parse html)
        p (.select doc "p")
        text (.text (first p))
        description (subs text 0 (min 155 (count text))) ]
      description))