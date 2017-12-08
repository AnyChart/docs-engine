(ns wiki.generator.markdown
  (:import [org.apache.commons.lang3 StringEscapeUtils])
  (:require [markdown.core :refer [md-to-html-string]]
            [markdown.transformers :refer [transformer-vector]]
            [selmer.parser :refer [render-file]]
            [taoensso.timbre :as timbre :refer [info error]]
            [wiki.data.playground :as pg-data]
            [wiki.components.notifier :as notifications]
            [wiki.generator.phantom.core :as phantom]
            [version-clj.core :as version-clj]
            [wiki.util.utils :as utils]
            [hiccup.core :as h]
            [wiki.views.iframe :as iframe-view]
            [clojure.string :as string]))

(defn get-tags [text]
  (if-let [matches (re-matches #"(?s).*(\{tags\}(.*)\{tags\}[\r\n]?).*" text)]
    (let [source (second matches)
          tags-str (last matches)
          tags (re-seq #"[^\s,]+" tags-str)]
      {:html (clojure.string/replace text source "") :tags tags})
    {:html text :tags []}))

;(defn- build-sample-embed [version playground sample-path custom-settings]
;  (let [width (:width custom-settings)
;        height (:height custom-settings)
;        div-style (if (not (= width nil))
;                    (str "style='width:" (+ width 10) "px;'")
;                    "")
;        style (if (and (not (= width nil))
;                       (not (= height nil)))
;                (str "style='width:" width "px;height:" height "px;'")
;                "")]
;    (str
;      "<div class='iframe' " div-style ">
;       <div class='no-overflow'>
;       <iframe " style " src='//" playground "/" version "/samples/" sample-path "-iframe'></iframe></div>
;       <div class='btns'>
;         <a class='btn-playground btn' target='_blank' href='//" playground "/" version "/samples/" sample-path "'><i class='ac ac-play'></i> Playground</a>
;       </div></div>")))

;; =====================================================================================================================
;; Sample insertion
;; =====================================================================================================================
(defn get-code [id code scripts sample version-key]
  (condp = (count scripts)
    0 (str "(function(){
            if (anychart.hasOwnProperty('theme')) {anychart.theme('defaultTheme');}\n"
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
       (get-code id code scripts sample version-key)
       "}"))

(defn build-sample-div-old [notifier page-url id version samples playground sample-path custom-settings page-report]
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
        div-style (str "style='width:" width ";'")
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
                (clojure.string/replace #"'#container'" (str "'#" full-id "'"))
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
        (swap! page-report (fn [page-report] (update page-report :sample-not-available conj page-url)))
        (info "Sample isn't available:  " page-url url id full-id)
        (format "<div class=\"alert alert-warning\"><strong>Sample not available!</strong><p>%s</p></div>" url)))))

;; =====================================================================================================================
;; Sample insertion EMBEDED IFRAME
;; =====================================================================================================================

(defn doc-code [id sample]
  (let [html (str "<!DOCTYPE html>" (h/html (iframe-view/iframe (assoc sample
                                                                  :markup "<div id='container'/>"
                                                                  :style "html, body, #container {width:100%;height:100%;margin:0;padding:0;}"))))
        ;html (string/replace html #"/" "\\\\/")
        ;html (string/replace html #"\"" "\\\\\"")
        ;html (string/replace html #"\\n" "\\\\\\\\n")
        ;html (string/replace html #"\n" "\\\\n")
        html (StringEscapeUtils/escapeEcmaScript html)]
    (str "(function(){\n"
         "var doc = document.getElementById('iframe-" id "').contentWindow.document;\n"
         "doc.open();\n"
         "doc.write(\"" html "\");\n"
         "doc.close();\n})();")))


(defn build-sample-div [notifier page-url id version samples playground sample-path custom-settings page-report]
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
        div-style (str "style='width:" width ";'")
        style (str "style='display:block;position:relative;margin:0px;height:" height ";'")
        url (str "/samples/" (StringEscapeUtils/unescapeHtml4 sample-path))
        ;sample (pg-data/sample-by-url pg-jdbc (:id pg-version) url)
        sample (first (filter #(= url (:url %)) samples))
        full-id (str "container" id)]
    (if (some? sample)
      (let [code (doc-code id sample)]
        (render-file "templates/doc_sample.selmer" (assoc sample
                                                     :div-style div-style
                                                     :style style
                                                     :id id
                                                     :code code
                                                     :playground playground
                                                     :version version
                                                     :sample-path sample-path
                                                     :engine-version version)))
      (do
        (notifications/sample-not-available notifier version page-url)
        (swap! page-report (fn [page-report] (update page-report :sample-not-available conj page-url)))
        (info "Sample isn't available:  " page-url url id full-id)
        (format "<div class=\"alert alert-warning\"><strong>Sample not available!</strong><p>%s</p></div>" url)))))


;; =====================================================================================================================
;; Sample insertion EMBEDED IFRAME END
;; =====================================================================================================================


(defn generate-img [generator-config sample-path page-url samples version]
  (let [url (str "/samples/" (StringEscapeUtils/unescapeHtml4 sample-path))
        sample (first (filter #(= url (:url %)) samples))]
    (try (phantom/generate-img (:phantom-engine generator-config)
                               (:generator generator-config)
                               (:images-dir generator-config)
                               page-url
                               version
                               sample)
         (catch Exception e
           (prn "ERROR img gen: " e)))))

(defn- sample-transformer [id-counter notifier page-url version samples generator-config generate-images page-report]
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
             (when (and (:generate-images generator-config)
                        generate-images
                        (= 0 @id-counter))
               (generate-img generator-config sample-path page-url samples version))
             (clojure.string/replace text
                                     source
                                     ;(build-sample-embed version playground sample-path custom-settings)
                                     (build-sample-div notifier
                                                       page-url
                                                       (swap! id-counter inc)
                                                       version samples
                                                       (:playground generator-config)
                                                       sample-path custom-settings page-report)))
           (catch Exception _
             (do
               (notifications/sample-parsing-error notifier version page-url)
               (swap! page-report (fn [page-report] (update page-report :sample-parsing-error conj page-url)))
               (format "<div class=\"alert alert-danger\"><strong>Sample parsing error!</strong><p>%s</p></div>"
                       (clojure.string/trim text)))))
         text))
     state]))

(defn- add-api-links [text version reference api-versions api-default-version]
  (let [real-version (if (some #{version} api-versions)
                       version api-default-version)]
    (clojure.string/replace text
                            #"\{api:([^}]+)\}([^{]+)\{api\}"
                            (fn [[_ link title]]
                              (str "<a class='method' href='//" reference "/" real-version "/" link "'>" title "</a>")))))

(defn- add-pg-links [text version playground]
  (let [real-version (if (utils/released-version? version)
                       version "latest")]
    (clojure.string/replace text
                            #"\{pg:([^}]+)\}([^{]+)\{pg\}"
                            (fn [[_ link title]]
                              (let [link (clojure.string/replace link #"<i>|</i>|<em>|</em>" "_")
                                    link-parts (clojure.string/split link #"/")
                                    project (first link-parts)
                                    link-url (clojure.string/join "/" (drop 1 link-parts))]
                                (str "<a href='//" playground "/" project "/" real-version "/" link-url "'>" title "</a>"))))))

(defn- add-tags [html tags]
  (let [tags-html (str "<div class='tags'>" (apply str (map #(str "<span>" % "</span>") tags)) "</div>")
        h1 "</h1>"
        index (+ (.indexOf html h1) (count h1))]
    (str (subs html 0 index) tags-html (subs html index))))

(defn branch-name-transformer [version]
  (fn [text state]
    [(clojure.string/replace text #"\{branch_name\}" version) state]))

(defn- image-format-error [notifier version page-url text page-report]
  (info "image error:" page-url text)
  (notifications/image-format-error notifier version page-url)
  (swap! page-report (fn [page-report] (update page-report :image-format-error conj page-url)))
  (format "<div class=\"alert alert-danger\"><strong>Image format error!</strong><p>%s</p></div>"
          (clojure.string/trim text)))

(defn- image-checker [notifier page-url version page-report]
  (fn [text state]
    [(if (or (:code state) (:codeblock state))
       text
       (try
         (if-let [[_ _ params] (re-matches #".*(!\[[^\]]*\]\s*\(([^\)]*)\)).*" text)]
           (if (re-matches #"[^\s]+\s*(\"[^\"]+\")?\s*(\w+\s*=\s*[\w\"%]+\s*)*" params)
             text
             (image-format-error notifier version page-url params page-report))
           text)
         (catch Exception _ (image-format-error notifier version page-url "exception caught" page-report))))
     state]))

(defn to-html [notifier page-url source version samples api-versions
               {:keys [playground playground-base reference reference-default-version] :as generator-config}
               generate-images page-report]
  (let [{tags :tags html-without-tags :html} (get-tags source)
        html (-> (md-to-html-string html-without-tags
                                    :heading-anchors true
                                    :reference-links? true
                                    :replacement-transformers (concat [(branch-name-transformer version)
                                                                       (image-checker notifier page-url version page-report)]
                                                                      transformer-vector
                                                                      [(sample-transformer (atom 0) notifier page-url version samples
                                                                                           generator-config generate-images page-report)]))
                 (add-api-links version reference api-versions reference-default-version)
                 (add-pg-links version playground-base))
        html-tags (if (empty? tags) html
                                    (add-tags html tags))]
    {:html html-tags :tags tags}))
