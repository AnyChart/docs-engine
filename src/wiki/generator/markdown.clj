(ns wiki.generator.markdown
  (:import [org.apache.commons.lang3 StringEscapeUtils])
  (:require [markdown.core :refer [md-to-html-string]]
            [markdown.transformers :refer [transformer-vector]]
            [selmer.parser :refer [render-file]]
            [taoensso.timbre :as timbre :refer [info error]]
            [wiki.data.playground :as pg-data]
            [wiki.components.notifier :as notifications]
            [version-clj.core :as version-clj]))

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
         <a class='btn-playground btn jsfiddle-btn' target='_blank'><i class='ac ac-jsfiddle'></i> JsFiddle</a>
         <a class='btn-playground btn' target='_blank' href='//" playground "/" version "/samples/" sample-path "-plain'><i class='ac ac-play'></i> Playground</a>
       </div></div>")))

(defn get-code [id code scripts sample version-key]
  (condp = (count scripts)
    0 (str "(function(){
           anychart.theme(null);\n"
           (when (and (= id 1)
                      (> (version-clj/version-compare version-key "7.9.1") 0))
             "anychart.utils.hideTooltips(true);\n")
           (let [export (:exports sample)]
             (if (= export "chart")
               "var chart;\n"
               (str "var chart;\n" (when export (str "var " export ";\n")))))
           code
           "})();")
    (str "$.ajax({url: '" (first scripts) "', dataType: 'script', crossDomain: true, success:function(data, status, jqxhr){
      " (get-code id code (drop 1 scripts) sample version-key) "
      }});")))

(defn get-wrapped-code [id code scripts sample version-key]
  (str "sampleInit" id " = function(){"
       (get-code id code scripts sample version-key )
       "}"))

(defn build-sample-div [notifier page-url id version samples playground sample-path custom-settings]
  (let [width (if (:width custom-settings)
                (if (string? (:width custom-settings))
                  (:width custom-settings)
                  (str (:width custom-settings) "px"))
                "100%")
        height (if (:height custom-settings)
                 (if (string? (:height custom-settings))
                   (:height custom-settings)
                   (str (:height custom-settings) "px"))
                 "400px")
        div-style  (str "style='width:" width ";'")
        style (str "style='position:relative;margin:0px;height:" height ";'")
        url (str "/samples/" (StringEscapeUtils/unescapeHtml4 sample-path))
        ;sample (pg-data/sample-by-url pg-jdbc (:id pg-version) url)
        sample (first (filter #(= url (:url %)) samples))
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
                                                 :code (get-wrapped-code id code (:scripts sample) sample version)
                                                 :playground playground
                                                 :version version
                                                 :sample-path sample-path
                                                 :engine-version version)))
      (do
        (notifications/sample-not-available notifier version page-url)
        (info "Sample isn't available:  " page-url url id full-id)
        (format "<div class=\"alert alert-warning\"><strong>Sample not available!</strong><p>%s</p></div>" url)))))

(defn- sample-transformer [id-counter notifier page-url version samples playground]
  (fn [text state]
    [(if (or (:code state) (:codeblock state))
       text
       (if-let [matches (re-matches #".*(\{sample([^}]*)\}(.*)\{sample\}).*" text)]
         (try
           (let [sample-path (last matches)
                source (nth matches 1)
                custom-settings (read-string
                                  (clojure.string/replace (str "{" (nth matches 2) "}")
                                                          #"(\d+%)" "\"$1\""))]
            (clojure.string/replace text
                                    source
                                    ;(build-sample-embed version playground sample-path custom-settings)
                                    (build-sample-div notifier page-url (swap! id-counter inc) version samples playground
                                                      sample-path custom-settings)))
           (catch Exception _
             (do
               (notifications/sample-parsing-error notifier version page-url)
               (format "<div class=\"alert alert-danger\"><strong>Sample parsing error!</strong><p>%s</p></div>"
                       (clojure.string/trim text)))))
         text))
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

(defn branch-name-transformer [version]
  (fn [text state]
    [(clojure.string/replace text #"\{branch_name\}" version) state]))

(defn- image-format-error [notifier version page-url text]
  (info "image error:" page-url text)
  (notifications/image-format-error notifier version page-url)
  (format "<div class=\"alert alert-danger\"><strong>Image format error!</strong><p>%s</p></div>"
          (clojure.string/trim text)))

(defn- image-checker [notifier page-url version]
  (fn [text state]
    [(if (or (:code state) (:codeblock state))
       text
       (try
         (if-let [[_ _ params] (re-matches #".*(!\[[^\]]*\]\s*\(([^\)]*)\)).*" text)]
           (if (re-matches #"[^\s]+\s*(\"[^\"]+\")?\s*(\w+\s*=\s*[\w\"%]+\s*)*" params)
             text
             (image-format-error notifier version page-url params))
           text)
         (catch Exception _ (image-format-error notifier version page-url "exception caught"))))
     state]))

(defn to-html [notifier page-url source version samples playground reference api-versions api-default-version]
  (let [{tags :tags html-without-tags :html} (get-tags source)
        html (-> (md-to-html-string html-without-tags
                                    :heading-anchors true
                                    :reference-links? true
                                    :replacement-transformers (concat [(branch-name-transformer version)
                                                                       (image-checker notifier page-url version)]
                                                                      transformer-vector
                                                                      [(sample-transformer (atom 0) notifier page-url version samples playground)
                                                                       code-transformer]))
                 (add-api-links version reference api-versions api-default-version))
        html-tags (if (empty? tags) html
                                    (add-tags html tags))]
    {:html html-tags :tags tags}))
