(ns wiki.documents
  (:require [wiki.config :as config]
            [clojure.tools.logging :as log]
            [taoensso.carmine :as car]
            [wiki.data :refer (wcar*)]
            [clojure.java.io :refer (file)]))

(defn redis-documents-list-key [version]
  (str "docs_v_" version "_documents"))

(defn redis-document-key [version path]
  (str "docs_v_" version "_d_" path))

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
    (wcar* (car/set (redis-document-key version url) (.getAbsolutePath path)))))

(defn exists? [version url]
  (= 1 (wcar* (car/exists (redis-document-key version url)))))

(defn get-docs-for-group [docs group]
  {:name group
   :display_name (get-group-display-name group)
   :pages (sort-by :url (filter #(= group (:group %)) docs))})

(defn build-grouped-documents [version]
  (log/info "build documents tree for" version)
  (let [docs (map (fn [url] {:url url
                             :name (get-document-display-name url)
                             :group (get-group url)})
                  (wcar* (car/smembers (redis-documents-list-key version))))
        groups (sort-by :group (set (map #(:group %) docs)))
        grouped-docs (map #(get-docs-for-group docs %) groups)]
    (wcar* (car/set (redis-grouped-documents-key version) grouped-docs)))
  (log/info "done!"))

(defn grouped-documents [version]
  (wcar* (car/get (redis-grouped-documents-key version))))

(defn md-path [version url]
  (wcar* (car/get (redis-document-key version url))))


(defn update [version]
  (log/info "building documents for" version)
  (doseq [path (get-all-documents-from-fs version)]
    (process-document version path))
  (build-grouped-documents version)
  (log/info "done!"))