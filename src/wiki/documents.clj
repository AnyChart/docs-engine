(ns wiki.documents
  (:require [wiki.config :as config]
            [taoensso.carmine :as car]
            [wiki.data :refer (wcar*)]
            [clojure.java.io :refer (file)]
            [clojure.string :refer (trim-newline)]))

(defn redis-documents-list-key [version]
  (str "docs_v_" version "_documents"))

(defn redis-document-key [version path]
  (str "docs_v_" version "_d_" path))

(defn redis-group-meta-key [version path]
  (str "docs_v_" version "_g_" path))

(defn redis-grouped-documents-key [version]
  (str "docs_v_" version "_grouped_docs"))

(defn version-path [version]
  (str config/versions-path "/" version))

(defn get-all-documents-from-fs [version]
  (let [files (tree-seq
               (fn [f] (and (.isDirectory f) (not (.isHidden f))))
               (fn [d] (filter #(not (.isHidden %)) (.listFiles d)))
               (file (version-path version)))]
    (filter #(.endsWith (.getName %) ".md") files)))

(defn documents [version]
  (wcar* (car/smembers (redis-documents-list-key version))))

(defn clean [versions]
  (doseq [v versions]
    (map #(wcar* (car/del (redis-document-key %))) (documents v))
    (wcar* (car/del (redis-documents-list-key v)))))

(defn get-relative-path [version f]
  (clojure.string/replace (.getAbsolutePath f)
                          (clojure.string/re-quote-replacement
                           (str config/versions-path "/" version "/"))
                          ""))

(defn get-folder-url [path]
  (str (last (re-matches #"(.+)/index\.md$" path)) "/"))

(defn get-file-url [path]
  (last (re-matches #"(.+)\.md$" path)))

(defn get-group [url]
  (last (re-matches #"(.+)/.+$" url)))

(defn get-name [url]
  (last (re-matches #".*/(.+)$" url)))

(defn get-document-display-name [url]
  (clojure.string/replace (get-name (str "/" url)) "_" " "))

(defn get-document-info-from-fs [path]
  (let [content (slurp path)
        matches (re-matches #"(?s)(?m)(^\{[^\}]+\}).*" content)]
    (if matches
      (read-string (last matches))
      {})))

(defn get-group-info-from-fs [version path]
  (let [f (file (str config/versions-path "/" version "/" path "/group.cfg"))]
    (if (.exists f)
      (read-string (slurp f))
      {})))

(defn get-document-index [version url]
  (let [p (:index (:info (wcar* (car/get (redis-document-key version url)))))]
    (if (= p nil)
      1000
      p)))

(defn get-group-index [meta]
  (let [p (:index meta)]
    (if (= p nil)
      1000
      p)))

(defn title [url]
   (get-document-display-name url))

(defn get-group-display-name [group]
  (if (not (= nil group))
    (clojure.string/replace group "_" " ")
    nil))

(defn get-url [version path]
  (let [relative-path (get-relative-path version path)]
    (if (re-matches #".+/index\.md" relative-path)
      (get-folder-url relative-path)
      (get-file-url relative-path))))

(defn process-document [version path]
  (let [url (get-url version path)]
    (wcar* (car/sadd (redis-documents-list-key version) url))
    (wcar* (car/set (redis-document-key version url)
                    {:info ( get-document-info-from-fs (.getAbsolutePath path))
                     :path (.getAbsolutePath path)}))))

(defn exists? [version url]
  (= 1 (wcar* (car/exists (redis-document-key version url)))))

(defn get-docs-for-group [docs group]
  {:name (:group group)
   :index (get-group-index group)
   :display_name (get-group-display-name (:group group))
   :pages (sort-by (juxt :index :url) (filter #(= (:group group) (:group %)) docs))})

(defn build-grouped-documents [version]
  ;;(log/info "build docs tree for" version)
  (let [docs (map (fn [url] {:url url
                             :name (get-document-display-name url)
                             :index (get-document-index version url)
                             :group (get-group url)})
                  (wcar* (car/smembers (redis-documents-list-key version))))
        groups (sort-by :group (set (map #(:group %) docs)))
        groups-with-meta (map (fn [group]
                                (assoc (if group
                                         (get-group-info-from-fs version group)
                                         {}) :group group))
                              groups)
        grouped-docs (sort-by (juxt :index :name)
                              (map #(get-docs-for-group docs %) groups-with-meta))]
    (map (fn [meta]
           (wcar* (car/set (redis-group-meta-key version (:group meta)) meta)))
         groups-with-meta)
    (wcar* (car/set (redis-grouped-documents-key version) grouped-docs)))
  )

(defn grouped-documents [version]
  (wcar* (car/get (redis-grouped-documents-key version))))


(defn get-group-first-doc [version path]
  (let [grouped (grouped-documents version)
        group (first (filter #(= (:name %) path) grouped))]
    (:url (first (:pages group)))))

(defn md-path [version url]
  (:path (wcar* (car/get (redis-document-key version url)))))

(defn doc-content [doc]
  (let [code (clojure.string/replace-first doc #"\A(?s)(?m)(^\{[^\}]+\})" "")
        s (trim-newline code)]
    (loop [index 0]
      (if (= 0 (.length s))
        ""
        (let [ch (.charAt s index)]
          (if (or (= ch \newline) (= ch \return))
            (recur (inc index))
            (.. s (subSequence index (.length s)) toString)))))))

(defn get-content [path]
  (doc-content (slurp path)))

(defn update [version]
  (doseq [path (get-all-documents-from-fs version)]
    (process-document version path))
  (build-grouped-documents version))
