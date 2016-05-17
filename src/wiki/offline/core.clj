(ns wiki.offline.core
  (:import [java.io FileInputStream])
  (:require [clojure.java.io :refer [make-parents] :as io]
            [selmer.parser :refer [render-file add-tag!]]
            [wiki.components.redis :as redisca]
            [wiki.components.notifier :refer [notify-404]]
            [wiki.data.versions :as versions-data]
            [wiki.data.pages :as pages-data]
            [wiki.data.folders :as folders-data]
            [wiki.web.tree :refer [tree-view]]
            [net.cgrand.enlive-html :as html]
            [org.httpkit.client :as http]
            [me.raynes.fs :as fs]
            [cpath-clj.core :as cp]
            [wiki.offline.zip :as zip]
            [taoensso.timbre :as timbre :refer [info error]]))

(def ^:const doctype "<!doctype html>")
(def ^:const doctype-lenght (count doctype))

(defn remove-doctype [html]
  (if (= (-> html (subs 0 doctype-lenght) clojure.string/lower-case) doctype)
    (subs html doctype-lenght)
    html))

(defn add-doctype [html]
  (str doctype "\n" html))

(defn node2html [node]
  (apply str (html/emit* node)))

(defn html2node [html]
  (first (html/html-resource (java.io.StringReader. html))))

(defn get-file-name [url]
  (str (clojure.string/replace url #"[:\./]" "_") (fs/extension url)))

(defn copy-style [main-path]
  "replace paths in main.css: ../fonts -> fonts"
  (let [data (slurp (io/resource "public/main.css"))
        replaced-data (clojure.string/replace data #"../fonts" "fonts")]
    (spit (str main-path "/deps/main.css") replaced-data)))

(defn copy-from-resource [resource-dir output-dir]
  (doseq [[path uris] (cp/resources (io/resource resource-dir))
          :let [uri (first uris)]]
    (make-parents (str output-dir path))
    (with-open [in (io/input-stream uri)]
      (io/copy in (io/file (str output-dir path))))))

(defn create-dependencies [main-path]
  (fs/delete-dir main-path)
  (fs/mkdirs main-path)
  (fs/mkdirs (str main-path "/samples/js"))
  (fs/mkdir (str main-path "/deps"))
  (spit (str main-path "/deps/main.min.js")
        (slurp (io/resource "public/main.min.js")))
  (spit (str main-path "/deps/local.js")
        (slurp (io/resource "local/local.js")))
  (copy-style main-path)
  (copy-from-resource "public/fonts" (str main-path "/deps/fonts"))
  (copy-from-resource "public/icons" (str main-path "/deps/icons"))
  (copy-from-resource "public/i" (str main-path "/deps/i")))

(defn download-file [url absolute-name]
  ;(info "Load file: " url absolute-name)
  (try
    (with-open [in (io/input-stream url)
                out (io/output-stream absolute-name)]
      (io/copy in out))
    (catch Exception e
      (error "Download file failed: " url e))))

(defn start-load-link-if-need [url absolute-name links]
  (swap! links (fn [links] (if (nil? (get links url))
                             (assoc links url (future (download-file url absolute-name)))
                             links))))

(defn get-url [src]
  (cond
    (.startsWith src "http") src
    (.startsWith src "//") (str "http://" (subs src 2))
    :else (do
            (info "get-url: unknown src:" src)
            src)))

(defn replace-iframe-script [samples-path links node]
  (if-let [src (-> node :attrs :src)]
    (let [url (get-url src)
          name (get-file-name url)
          absolute-name (str samples-path "js/" name)]
      (start-load-link-if-need url absolute-name links)
      (assoc-in node [:attrs :src] (str "js/" name)))
    node))

(defn replace-iframe-link [samples-path links node]
  (if-let [src (-> node :attrs :href)]
    (let [url (get-url src)
          name (get-file-name url)
          absolute-name (str samples-path "js/" name)]
      (start-load-link-if-need url absolute-name links)
      (assoc-in node [:attrs :href] (str "js/" name)))
    node))

(defn replace-iframe-js [tree samples-path links]
  (html/at tree
           [:script] (partial replace-iframe-script samples-path links)
           [:link] (partial replace-iframe-link samples-path links)
           [:head] (html/prepend (html/html [:meta {:charset "utf-8"}]))))

(defn process-iframe [html samples-path links]
  (-> html
      remove-doctype
      html2node
      (replace-iframe-js samples-path links)
      node2html
      add-doctype))

(defn load-iframe [iframe samples-path links]
  (let [{:keys [body error]} @(http/get (:url iframe))]
    (if error
      (timbre/error "Error loading iframe " (:url iframe) error)
      (let [prepared-html (process-iframe body samples-path links)
            file-name (str samples-path (:name iframe) ".html")]
        (make-parents file-name)
        (spit file-name prepared-html)))))

(defn iframe-data [path]
  (let [index (.lastIndexOf path "/")
        name (subs path (inc index))
        url (str "http://" (subs path 2))]
    {:path path
     :name name
     :url  url}))

(defn replace-iframe-node [main-path path links node]
  (let [iframe (iframe-data (-> node :attrs :src))]
    (load-iframe iframe (str main-path "/samples/") links)
    (assoc-in node [:attrs :src] (str path "samples/" (:name iframe) ".html"))))

(defn replace-external-links [html]
  (-> html (clojure.string/replace #"href=\"//" "href=\"http://")))

(defn add-html [path]
  (let [parts (clojure.string/split path #"#")]
    (if (= 2 (count parts))
      (str (first parts) ".html#" (second parts))
      (str path ".html"))))

(defn replace-a-node [node]
  "add .html to relative paths"
  (let [href (-> node :attrs :href)]
    (if (and href
             (or (.startsWith href "./")
                 (.startsWith href "../"))
             (not (re-find #"\.(\w+)" href)))
      (assoc-in node [:attrs :href] (add-html href))
      node)))

(defn replace-img-node [main-path path links node]
  (let [url (-> node :attrs :src)]
    (if (and url
             (or (.startsWith url "http://")
                 (.startsWith url "//")))
      (let [url (get-url url)
            name (get-file-name url)
            absolute-name (str main-path "/deps/" name)]
        (start-load-link-if-need url absolute-name links)
        (assoc-in node [:attrs :src] (str path "deps/" name)))
      node)))

(defn replace-tags [tree main-path path links]
  (html/at tree
           [:div.iframe :iframe] (partial replace-iframe-node main-path path links)
           [:a] replace-a-node
           [:img] (partial replace-img-node main-path path links)))

(defn get-relative-prefix-path [page-url]
  (let [parts (clojure.string/split page-url (re-pattern "/"))]
    (condp = (count parts)
      1 "./"
      2 "../"
      3 "../../"
      4 "../../../")))

(defn save-page [page tree version versions main-path links]
  (let [path (get-relative-prefix-path (:url page))
        file-name (str main-path "/" (:url page) ".html")
        html (render-file "templates/local-page.selmer" {:version  (:key version)
                                                         ;:actual-version (first versions)
                                                         ;:old            (not= (first versions) (:key version))
                                                         :tree     tree
                                                         :url      (:url page)
                                                         :title    (:full_name page)
                                                         :page     page
                                                         :versions versions
                                                         :path     path})
        processed-html (-> html
                           html2node
                           (replace-tags main-path path links)
                           node2html
                           replace-external-links
                           add-doctype)]
    (info "Page: " file-name)
    (make-parents file-name)
    (spit file-name processed-html)))

(defn generate-zip [config jdbc version]
  (let [tree (versions-data/tree-data jdbc (:id version))
        pages (pages-data/all-pages-by-version jdbc (:id version))
        ;test-pages (take 5 pages)
        versions (versions-data/versions jdbc)
        zip-dir (:zip-dir config)
        main-path (str zip-dir "/" (:key version))
        zip-path (str main-path ".zip")
        links (atom {})]
    (info "Start generating offline docs: " version main-path)
    (create-dependencies main-path)
    (doall (pmap #(save-page % tree version versions main-path links) pages))
    (doseq [key-val @links] (-> key-val val deref))
    (zip/zip-folder main-path
                    zip-path
                    (str "/" (:key version)))
    (info "End generating offline docs: " zip-path)
    (versions-data/update-zip jdbc (:id version) (FileInputStream. zip-path))
    zip-path))





