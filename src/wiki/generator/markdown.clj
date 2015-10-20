(ns wiki.generator.markdown
  (:require [markdown.core :refer [md-to-html-string]]))

(defn- build-sample-embed [version playground sample-path custom-settings]
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
     <a target='_blank' href='" playground "/" version "/samples/" sample-path "-plain'>Launch in playground</a>
     <iframe " style "src='" playground "/" version "/samples/" sample-path "-iframe'></iframe></div>")))

(defn- sample-transformer [version playground]
  (fn [text state]
    [(if (or (:code state) (:codeblock state))
       text
       (let [matches (re-matches #".*(\{sample([^}]*)\}(.*)\{sample\}).*" text)
             sample-path (last matches)
             source (nth matches 1)
             custom-settings (read-string (str "{" (nth matches 2) "}"))]
         (if sample-path
           (clojure.string/replace text
                                   source
                                   (build-sample-embed version playground
                                                       sample-path custom-settings))
           text)))
     state]))

(defn- add-api-links [text version reference]
  (clojure.string/replace text
                          #"\{api:([^}]+)\}([^{]+)\{api\}"
                          (fn [[_ link title]]
                            (str "<a href='" reference "/" version "/" link "'>" title "</a>"))))

(defn to-html [source version playground reference]
  (-> (md-to-html-string source
                         :heading-anchors true
                         :custom-transformers [(sample-transformer version playground)])
      (add-api-links version reference)))
