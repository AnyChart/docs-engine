(ns wiki.util.utils
  (:require [clojure.string :as s])
  (import [org.jsoup Jsoup]))

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

(defn remove-tags
  "subs - for optimization to not parse all html"
  [html]
  (let [replaced-html (.text (Jsoup/parse (subs html 0 (min 1000 (count html)))))]
    (subs replaced-html 0 (min 155 (count replaced-html)))))