(ns wiki.generator.markdown
  (:import [org.apache.commons.lang3 StringEscapeUtils])
  (:require [wiki.components.notifier :as notifications]
            [wiki.generator.phantom.core :as phantom]
            [wiki.util.utils :as utils]
            [wiki.views.iframe :as iframe-view]
            [wiki.config.core :as c]
            [markdown.core :refer [md-to-html-string]]
            [markdown.transformers :refer [transformer-vector]]
            [selmer.parser :refer [render-file]]
            [taoensso.timbre :as timbre :refer [info error]]
            [hiccup.core :as h]
            [clojure.string :as string]))


(defn get-tags [text]
  (if-let [matches (re-matches #"(?s).*(\{tags\}(.*)\{tags\}[\r\n]?).*" text)]
    (let [source (second matches)
          tags-str (last matches)
          tags (re-seq #"[^\s,]+" tags-str)]
      {:html (string/replace text source "") :tags tags})
    {:html text :tags []}))


;; =====================================================================================================================
;; Sample insertion EMBEDED IFRAME
;; =====================================================================================================================
(defn doc-code [id sample]
  (let [html (str "<!DOCTYPE html>" (h/html (iframe-view/iframe sample)))
        html (StringEscapeUtils/escapeEcmaScript html)]
    (str "(function(){\n"
         "var doc = document.getElementById('iframe-" id "').contentWindow.document;\n"
         "doc.open();\n"
         "doc.write(\"" html "\");\n"
         "doc.close();\n})();")))


(defn build-sample-div [notifier page-url id version samples sample-path custom-settings page-report]
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
        url (str "samples/" (StringEscapeUtils/unescapeHtml4 sample-path))
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
                                                     :playground (c/playground-project)
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
  (let [url (str "samples/" (StringEscapeUtils/unescapeHtml4 sample-path))
        sample (first (filter #(= url (:url %)) samples))]
    (try (phantom/generate-img (:phantom-engine generator-config)
                               (:generator generator-config)
                               (:images-dir generator-config)
                               page-url
                               version
                               sample)
         (catch Exception e (timbre/error "ERROR img gen: " e)))))


(defn- sample-transformer [*id-counter *links notifier page-url version samples generator-config generate-images page-report]
  (fn [text state]
    [(if (or (:code state) (:codeblock state))
       text
       (if-let [matches (re-matches #".*(\{sample([^}]*)\}(.*)\{sample\}).*" text)]
         (try
           (let [sample-path (last matches)
                 source (nth matches 1)
                 custom-settings (read-string
                                   (string/replace (str "{" (nth matches 2) "}")
                                                   #"(\d+%)" "\"$1\""))]
             (when (and (:generate-images generator-config)
                        generate-images
                        (= 0 @*id-counter))
               (generate-img generator-config sample-path page-url samples version))
             (swap! *links update-in [:samples] conj {:path sample-path})
             (string/replace text
                             source
                             (build-sample-div notifier
                                               page-url
                                               (swap! *id-counter inc)
                                               version samples
                                               sample-path custom-settings page-report)))
           (catch Exception e
             (do
               (prn "SAMPLE PARSING ERROR ========================================================================================================")
               (prn e)
               (notifications/sample-parsing-error notifier version page-url)
               (swap! page-report (fn [page-report] (update page-report :sample-parsing-error conj page-url)))
               (format "<div class=\"alert alert-danger\"><strong>Sample parsing error!</strong><p>%s</p></div>"
                       (string/trim text)))))
         text))
     state]))


(defn- add-api-links [text version api-versions api-default-version *links]
  (let [real-version (if (some #{version} api-versions)
                       version
                       api-default-version)]
    (string/replace text
                    #"\{api:([^}]+)\}([^{]+)\{api\}"
                    (fn [[_ link title]]
                      (swap! *links update-in [:api] conj {:title title
                                                           :url   link})
                      (str "<a class='method' target='_blank' href='//" (c/reference) "/" real-version "/" link "'>" title "</a>")))))


(defn- add-pg-links [text version *links]
  (let [real-version (if (utils/released-version? version)
                       version "develop")]
    (string/replace text
                    #"\{pg:([^}]+)\}([^{]+)\{pg\}"
                    (fn [[_ link title]]
                      (swap! *links update-in [:pg] conj {:title title
                                                          :url   link})
                      (let [link (string/replace link #"<i>|</i>|<em>|</em>" "_")
                            link-parts (string/split link #"/")
                            project (first link-parts)
                            link-url (string/join "/" (drop 1 link-parts))]
                        (str "<a target='_blank' href='//" (c/playground) "/" project "/" real-version "/" link-url "'>" title "</a>"))))))


(defn- add-tags [html tags]
  (let [tags-html (str "<div class='tags'>" (apply str (map #(str "<span>" % "</span>") tags)) "</div>")
        h1 "</h1>"
        index (+ (.indexOf html h1) (count h1))]
    (str (subs html 0 index) tags-html (subs html index))))


(defn branch-name-transformer [version]
  (fn [text state]
    [(string/replace text #"\{branch_name\}" version) state]))


(defn- image-format-error [notifier version page-url text page-report]
  (info "image error:" page-url text)
  (notifications/image-format-error notifier version page-url)
  (swap! page-report (fn [page-report] (update page-report :image-format-error conj page-url)))
  (format "<div class=\"alert alert-danger\"><strong>Image format error!</strong><p>%s</p></div>"
          (string/trim text)))


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


(defn to-html [notifier
               page-url
               source
               version
               samples
               api-versions
               {:keys [reference-default-version] :as generator-config}
               generate-images
               page-report]
  (let [*links (atom {:samples []
                      :api     []
                      :pg      []})
        {tags :tags html-without-tags :html} (get-tags source)
        html (-> (md-to-html-string html-without-tags
                                    :heading-anchors true
                                    :reference-links? true
                                    :replacement-transformers (concat [(branch-name-transformer version)
                                                                       (image-checker notifier page-url version page-report)]
                                                                      transformer-vector
                                                                      [(sample-transformer (atom 0) *links notifier page-url version samples
                                                                                           generator-config generate-images page-report)]))
                 (add-api-links version api-versions reference-default-version *links)
                 (add-pg-links version *links))
        html-tags (if (empty? tags) html
                                    (add-tags html tags))]
    ;; (println @*links)
    {:html html-tags :tags tags}))
