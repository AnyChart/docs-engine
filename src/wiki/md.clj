(ns wiki.md
  (:require [selmer.filters :refer [add-filter!]]
            [clojure.contrib.str-utils2 :as str-utils]
            [markdown.core :refer [md-to-html-string]]))

(def playground-path (atom ()))
(def reference-path (atom ()))

(add-filter! :safe (fn [x] [:safe x]))

(defn set-reference-path [env]
  (swap! reference-path (fn [val & args]
                          (case env
                            "docs.anychart.com" "//api.anychart.com/"
                            "docs.anychart.dev" "//api.anychart.dev/"
                            "docs.anychart.stg" "//api.anychart.stg/"
                            "//api.anychart.stg/"))))

(defn set-playground-path [env]
  (set-reference-path env)
  (swap! playground-path (fn [val & args]
                           (case env
                             "docs.anychart.com" "//playground.anychart.com/acdvf-docs"
                             "docs.anychart.dev" "//playground.anychart.dev/acdvf-docs"
                             "docs.anychart.stg" "//playground.anychart.stg/acdvf-docs"
                             "//playground.anychart.stg/acdvf-docs"))))

(defn build-sample-embed [sample-path custom-settings]
  (let [width (:width custom-settings)
        height (:height custom-settings)
        div-style (if (not (= width nil))
                    (str "style='width:" (+ width 10) "px;'")
                    "")
        style (if (and (not (= width nil))
                       (not (= height nil)))
                (str "style='width:" width "px;height:" height "px;'")
                "style='margin-left:50px; margin-right: 50px;'")]
    (str
     "<div class='sample' " div-style ">
     <p>Live sample</p>
     <a target='_blank' href='" @playground-path "/{{VERSION}}/samples/" sample-path "-plain'>Launch in playground</a>
     <iframe " style "src='" @playground-path "/{{VERSION}}/samples/" sample-path "-iframe'></iframe></div>")))

(defn sample-transformer [text state]
  [(if (or (:code state) (:codeblock state))
     text
     (let [matches (re-matches #".*(\{sample([^}]*)\}(.*)\{sample\}).*" text)
           sample-path (last matches)
           source (nth matches 1)
           custom-settings (read-string (str "{" (nth matches 2) "}"))]
       (if sample-path
         (str-utils/replace text source (build-sample-embed sample-path custom-settings))
         text)))
   state])

(defn build-reference-link [title link]
  (let [link (if (re-find #"\#.*" link)
               (clojure.string/replace link #"\#" ".html#")
               (str link ".html"))]
    (str "<a href='" @reference-path "{{VERSION}}/" link "'>" title "</a>")))

(defn api-reference-transformer [text state]
  [(if (or (:code state) (:codeblock state))
     text
     (let [matches (re-matches #".*(\{api:([^}]+)\}(.*)\{api\}).*" text)
           source (nth matches 1)
           link (nth matches 2)
           title (last matches)]
       (if matches
         (str-utils/replace text source (build-reference-link title link))
         text)))
   state])

(defn convert-markdown [version content env]
  (set-playground-path env)
  (clojure.string/replace (md-to-html-string content
                                             :heading-anchors true
                                             :custom-transformers [sample-transformer
                                                                   api-reference-transformer])
                          #"\{\{VERSION\}\}" version))
