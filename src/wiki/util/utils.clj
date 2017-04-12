(ns wiki.util.utils
  (:require [clojure.string :as s])
  (import [org.jsoup Jsoup]))

(defn deep-merge [a b]
  (merge-with (fn [x y]
                (cond (map? y) (deep-merge x y)
                      (vector? y) (concat x y)
                      :else y))
              a b))

(defn format-exception [e]
  (str e "\n\n" (apply str (interpose "\n" (.getStackTrace e)))))

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
    (s/join " | " parts)))

(defn drop-last-slash [s]
  (if (.endsWith s "/")
    (subs s 0 (dec (count s)))
    s))

(defn page-description [html]
  (when html
    (when-let [doc (Jsoup/parse html)]
      (when-let [p (first (.select doc "p"))]
        (when-let [text (.text p)]
          (when (not-empty text)
            (subs text 0 (min 155 (count text)))))))))

;; select * from pages where version_id in (select id from versions where key = '7.13.0') AND content NOT LIKE '%sampleInit1%';
;; for og:image tag
(defn articles-without-samples [file-name]
  (let [articles (-> file-name slurp clojure.string/split-lines)
        articles-with-urls (map (fn [a] [a (str (name->url a) ".png")]) articles)]
    (doseq [a articles-with-urls]
      (println (a 0) " " (a 1)))))