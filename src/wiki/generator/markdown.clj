(ns wiki.generator.markdown
  (:require [markdown.core :refer [md-to-html-string]]))

(defn- build-tags-embed [tags]
  ;todo: put here tags' design
  (str "<div>" (clojure.string/join ", " tags) "</div>"))

(defn tags-transformer [all-tags]
  (fn [text state]
    [(if (or (:code state) (:codeblock state))
       text
       (if-let [matches (re-matches #".*(\{tags\}(.*)\{tags\}).*" text)]
         (let [source (second matches)
               tags-str (last matches)
               tags (re-seq #"[^\s,]+" tags-str)]
           (swap! all-tags concat tags)
           (if source
             (clojure.string/replace text source (build-tags-embed tags))
             text))
         text))
     state]))

(defn- build-sample-embed [version playground sample-path custom-settings]
  (let [width (:width custom-settings)
        height (:height custom-settings)
        div-style (if (not (= width nil))
                    (str "style='width:" (+ width 10) "px;'")
                    "")
        style (if (and (not (= width nil))
                       (not (= height nil)))
                (str "style='width:" width "px;height:" height "px;'")
                "")]
    (str
      "<div class='iframe' " div-style ">
       <div class='no-overflow'>
       <iframe " style " src='//" playground "/" version "/samples/" sample-path "-iframe'></iframe></div>
       <div class='btns'>
         <a class='btn-playground btn jsfiddle-btn' target='_blank'><i class='fa fa-jsfiddle'></i> JsFiddle</a> 
         <a class='btn-playground btn' target='_blank' href='//" playground "/" version "/samples/" sample-path "-plain'><i class='fa fa-play'></i> Playground</a>
       </div></div>")))

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

(defn- code-shifted? [text]
  (every? #(re-find #"(^\t\S)|(^\s{4}\S)" %)
          (clojure.string/split-lines text)))

(defn- code-transformer [text state]
  (if (and (or (:code state) (:codeblock state))
           (code-shifted? text))
    [(-> text
         (clojure.string/replace #"(?m)^\t" "")
         (clojure.string/replace #"(?m)^\s{4}" "")) state]
    [text state]))

(defn- add-api-links [text version reference api-versions api-default-version]
  (let [real-version (if (some #{version} api-versions)
                       version api-default-version)]
    (clojure.string/replace text
                            #"\{api:([^}]+)\}([^{]+)\{api\}"
                            (fn [[_ link title]]
                              (str "<a class='method' href='//" reference "/" api-default-version "/" link "'>" title "</a>")))))

(defn to-html [source version playground reference api-versions api-default-version]
  (let [tags (atom [])
        html (-> (md-to-html-string source
                                    :heading-anchors true
                                    :custom-transformers [(sample-transformer version playground)
                                                          code-transformer
                                                          (tags-transformer tags)])
                 (add-api-links version reference api-versions api-default-version))]
    {:html html :tags @tags}))
