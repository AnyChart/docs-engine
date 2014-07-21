(ns wiki.md
  (:require [selmer.filters :refer [add-filter!]]
            [clojure.contrib.str-utils2 :as str-utils]
            [markdown.core :refer [md-to-html-string]]))

(def playground-path (atom ()))

(add-filter! :safe (fn [x] [:safe x]))

(defn set-playground-path [env]
  (swap! playground-path (fn [val & args]
                           (case env
                             "docs.anychart.com" "//playground.anychart.com/acdvf-docs"
                             "docs.anychart.dev" "//playground.anychart.dev/acdvf-docs"
                             "docs.anychart.stg" "//playground.anychart.stg/acdvf-docs"
                             "//playground.anychart.stg/acdvf-docs"))))

(defn build-sample-embed [sample-path]
  (str
   "<div class='sample'>
     <p>Live sample</p>
     <a target='_blank' href='//" @playground-path "{{VERSION}}/samples/" sample-path "-plain'>Launch in playground</a>
     <iframe src='//" @playground-path "{{VERSION}}/samples/" sample-path "-iframe'></iframe></div>"))
(defn sample-transformer [text state]
  [(if (or (:code state) (:codeblock state))
     text
     (let [matches (re-matches #".*(\{sample\}(.*)\{sample\}).*" text)
           sample-path (nth matches 2)
           source (nth matches 1)]
       (if sample-path
         (str-utils/replace text source (build-sample-embed sample-path))
         text)))
   state])

(defn convert-markdown [version path env]
  (set-playground-path env)
  (clojure.string/replace (md-to-html-string (slurp path)
                                             :heading-anchors true
                                             :custom-transformers [sample-transformer])
                          #"\{\{VERSION\}\}" version))
