(ns wiki.offline.core
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
  (copy-style main-path)
  (copy-from-resource "public/fonts" (str main-path "/deps/fonts"))
  (copy-from-resource "public/icons" (str main-path "/deps/icons"))
  (copy-from-resource "public/i" (str main-path "/deps/i")))

(defn download-file [url absolute-name]
  (info "Load file: " url absolute-name)
  (spit absolute-name (slurp url)))

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

(defn replace-iframe-dep [samples-path links node]
  (if-let [src (-> node :attrs :src)]
    (let [url (get-url src)
          name (fs/base-name src)
          absolute-name (str samples-path "js/" name)]
      (start-load-link-if-need url absolute-name links)
      (assoc-in node [:attrs :src] (str "js/" name)))
    node))

(defn replace-iframe-js [tree samples-path links]
  (html/at tree [:script] (partial replace-iframe-dep samples-path links)))

(defn process-iframe [html samples-path links]
  (-> html
      remove-doctype
      html2node
      (replace-iframe-js samples-path links)
      node2html
      add-doctype))

(defn load-iframe [iframe samples-path links]
  (let [html (-> @(http/get (:url iframe)) :body)
        prepared-html (process-iframe html samples-path links)
        file-name (str samples-path (:name iframe) ".html")]
    (make-parents file-name)
    (spit file-name prepared-html)))

(defn iframe-data [path]
  (let [index (.lastIndexOf path "/")
        name (subs path (inc index))
        url (str "http://" (subs path 2))]
    {:path path
     :name name
     :url  url}))

(defn find-iframes [data]
  (let [tags (-> data (html/select [:div.iframe :iframe]))
        iframes (map #(-> % :attrs :src iframe-data) tags)]
    iframes))

(defn replace-iframes-paths [page iframes]
  (reduce #(clojure.string/replace %1
                                   (re-pattern (:path %2))
                                   (str "../samples/" (:name %2)))
          page iframes))

(defn replace-iframe-node [main-path path links node]
  (let [iframe (iframe-data (-> node :attrs :src))]
    (load-iframe iframe (str main-path "/samples/") links)
    {:tag     :iframe
     :attrs   {:src (str path "samples/" (:name iframe) ".html")}
     :content nil}))

(defn replace-content [tree main-path path links]
  (html/at tree [:div.iframe :iframe] (partial replace-iframe-node main-path path links)))

(defn replace-external-links [html]
  (-> html (clojure.string/replace #"href=\"//" "href=\"http://")))

(defn add-html [path]
  (let [parts (clojure.string/split path #"#")]
    (if (= 2 (count parts))
      (str (first parts) ".html#" (second parts))
      (str path ".html"))))

(defn replace-local-link [node]
  "add .html to relative paths"
  (let [href (-> node :attrs :href)]
    (if (and href
             (or (= 0 (.indexOf href "./"))
                 (= 0 (.indexOf href "../")))
             (not (re-find #"\.(\w+)" href)))
      (assoc-in node [:attrs :href] (add-html href))
      node)))

(defn replace-local-links [tree]
  (html/at tree [:a] replace-local-link))

(defn get-relative-prefix-path [page-url]
  (let [parts (clojure.string/split page-url (re-pattern "/"))]
    (condp = (count parts)
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
                           (replace-content main-path path links)
                           replace-local-links
                           node2html
                           replace-external-links
                           add-doctype)]
    (info "Page: " file-name)
    (make-parents file-name)
    (spit file-name processed-html)))

(defn generate-zip [jdbc version]
  (let [tree (versions-data/tree-data jdbc (:id version))
        pages (pages-data/all-pages-by-version jdbc (:id version))
        test-pages (take 5 pages)
        versions (versions-data/versions jdbc)
        main-path (str (.getAbsolutePath (clojure.java.io/file "data")) "/zip/" (:key version))
        zip-path (str main-path ".zip")
        links (atom {})]
    (info "Start generating offline docs: " main-path version)
    (create-dependencies main-path)
    (doall (pmap #(save-page % tree version versions main-path links) pages))
    (doseq [key-val @links] (-> key-val val deref))
    (zip/zip-folder main-path
                    zip-path
                    (str "/" (:key version)))
    (info "End generating offline docs: " zip-path)
    zip-path))





