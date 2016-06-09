(ns wiki.generator.markdown
  (:import [org.apache.commons.lang3 StringEscapeUtils])
  (:require [markdown.core :refer [md-to-html-string]]
            [selmer.parser :refer [render-file]]
            [taoensso.timbre :as timbre :refer [info error]]
            [wiki.data.playground :as pg-data]))

(defn get-tags [text]
  (if-let [matches (re-matches #"(?s).*(\{tags\}(.*)\{tags\}[\r\n]?).*" text)]
    (let [source (second matches)
          tags-str (last matches)
          tags (re-seq #"[^\s,]+" tags-str)]
      {:html (clojure.string/replace text source "") :tags tags})
    {:html text :tags []}))

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

(defn get-code [id code scripts sample]
  (condp = (count scripts)
    0 (str "(function(){
           anychart.theme(null);\n"
           (when (= id 1) "anychart.utils.hideTooltips(true);\n")
           (let [export (:exports sample)]
             (if (= export "chart")
               "var chart;\n"
               (str "var chart;\n" (when export (str "var " export ";\n")))))
           code
          "})();")
    (str "$.ajax({url: '" (first scripts) "', dataType: 'script', crossDomain: true, success:function(data, status, jqxhr){
      " (get-code id code (drop 1 scripts) sample) "
      }});")))

(defn build-sample-div [id version pg-jdbc pg-version playground sample-path custom-settings]
  (let [width (:width custom-settings)
        height (:height custom-settings)
        div-style (if (not (= width nil))
                    (str "style='width:" (+ width 10) "px;'")
                    "")
        style (if (and (not (= width nil))
                       (not (= height nil)))
                (str "style='position:relative;margin:0px;width:" width "px;height:" height "px;'")
                "style='position:relative;margin:0px;'")
        url (str "/samples/" (StringEscapeUtils/unescapeHtml4 sample-path))
        sample (pg-data/sample-by-url pg-jdbc (:id pg-version) url)
        full-id (str "container" id)]
    (if (some? sample)
      (let [code
            (-> (:code sample)
                (clojure.string/replace #"\(\s*\"container\"" (str "(\"" full-id "\""))
                (clojure.string/replace #"\(\s*'container'\s*\)" (str "('" full-id "')"))
                (clojure.string/replace #":\s*\"container\"" (str ": \"" full-id "\""))
                (clojure.string/replace #" container\." (str " " full-id "."))
                (clojure.string/replace #"=\s*\"container\"" (str "=\"" full-id "\""))
                (clojure.string/replace #"=\s*'container'" (str "='" full-id "'"))
                (clojure.string/replace #"getElementsByTagName\(.body.\)\[0\]" (str "getElementById('iframe" id "')"))
                (clojure.string/replace #"\$\(.body.\)" "\\$('.iframe-tag')")
                (clojure.string/replace #"var\s+chart\s*=" "chart =")
                (clojure.string/replace #"\"fixed\"" "\"absolute\"")
                (clojure.string/replace #"anychart\.onDocumentReady" "setTimeout"))]
        (render-file "templates/sample.selmer" (assoc sample
                                                 :div-style div-style
                                                 :style style
                                                 :id id
                                                 :code (get-code id code (:scripts sample) sample)
                                                 :version (:key pg-version)
                                                 :playground playground
                                                 :version version
                                                 :sample-path sample-path
                                                 :engine-version (or (:engine_version pg-version)
                                                                     (:key pg-version)))))
      (do (info "Sample isn't available:  " pg-version url id full-id)
        ""))))

(defn- sample-transformer [id-counter version pg-jdbc pg-version playground]
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
                                   ;(build-sample-embed version playground sample-path custom-settings)
                                   (build-sample-div (swap! id-counter inc) version pg-jdbc pg-version playground
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

(defn- add-tags [html tags]
  (let [tags-html  (str "<div class='tags'>" (apply str (map #(str "<span>" % "</span>") tags)) "</div>")
        h1 "</h1>"
        index (+ (.indexOf html h1) (count h1))]
    (str (subs html 0 index) tags-html (subs html index))))

(defn to-html [source version pg-jdbc pg-version playground reference api-versions api-default-version]
  (let [{tags :tags html-without-tags :html} (get-tags source)
        html (-> (md-to-html-string html-without-tags
                                    :heading-anchors true
                                    :custom-transformers [(sample-transformer (atom 0) version pg-jdbc pg-version playground)
                                                          code-transformer])
                 (add-api-links version reference api-versions api-default-version))
        html-tags (if (empty? tags) html
                                    (add-tags html tags))]
    {:html html-tags :tags tags}))
