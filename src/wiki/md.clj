(ns wiki.md
  (:require [selmer.filters :refer [add-filter!]]
            [clojure.contrib.str-utils2 :as str-utils]
            [markdown.core :refer [md-to-html-string]]
            [wiki.config :refer [config]]))

(add-filter! :safe (fn [x] [:safe x]))

(defn build-sample-embed [sample-path]
  (str
   "<div class='sample'>
     <p>Live sample</p>
     <a target='_blank' href='//" (:playground config) "/acdvf-docs/{{VERSION}}/samples/" sample-path "'>Launch in playground</a>
     <iframe src='//" (:playground config) "{{VERSION}}/samples/" sample-path "-iframe'></iframe></div>"))

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

(defn convert-markdown [version path]
  (clojure.string/replace (md-to-html-string (slurp path)
                                             :heading-anchors true
                                             :custom-transformers [sample-transformer])
                          #"\{\{VERSION\}\}" version))
