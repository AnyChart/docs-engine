(ns wiki.generator.struct
  (:require [wiki.generator.git :refer [file-last-commit-date]]
            [clojure.java.io :refer [file]]
            [clojure.string :as string]))

(defn- title [f]
  (-> f .getName
      (string/replace #"_" " ")
      (string/replace #"\.md$" "")))

(defn- is-doc [f]
  (and (re-matches #".*\.md$" (-> f .getName .toLowerCase))
       (not= (-> f .getName .toLowerCase) "readme.md")))

(defn- get-name [f]
  (-> f .getName
      (string/replace #"\.md$" "")))

(defn- has-docs [f]
  (some #(or (is-doc %)
             (and (.isDirectory %)
                  (not (.isHidden %))
                  (has-docs %)))
        (.listFiles f)))

(defn- get-struct-as-list [path]
  (let [dir (file path)]
    (tree-seq #(and (.isDirectory %)
                    (not (.isHidden %))
                    (has-docs %)) #(.listFiles %) dir)))

(defn- fix-document-content [content]
  (-> content
      (string/replace-first #"\A(?s)(?m)(^\{[^\}]+\})" "")
      (string/trim-newline)))

(defn- read-document-config [content]
  (when-let [doc-header (re-matches #"(?s)(?m)(^\{[^\}]+\}).*" content)]
    (-> doc-header last read-string)))

(defn- create-document [base-path item]
  (let [content (slurp item)
        config (read-document-config content)
        page-title (title item)
        res {:name          (get-name item)
             :kind          :doc
             :title         page-title
             :content       (if config (fix-document-content content) content)
             :config        (merge {:index 1000} config)
             :last-modified (file-last-commit-date base-path (.getAbsolutePath item))}]
    res))

(declare build-struct)

(defn- create-folder [base-path item]
  (let [res {:name     (get-name item)
             :kind     :folder
             :title    (title item)
             :config   {:index 1000}
             :children (reduce (fn [res item]
                                 (build-struct res item base-path)) []
                               (.listFiles item))}]
    (if (.exists (file (str (.getAbsolutePath item) "/group.cfg")))
      (update res :config #(merge % (-> (str (.getAbsolutePath item) "/group.cfg")
                                        slurp
                                        read-string)))
      res)))

(defn- build-struct [items item base-path]
  (cond
    (.isHidden item) items
    (and (not (.isDirectory item))
         (is-doc item)) (conj items (create-document base-path item))
    (and (.isDirectory item)
         (get-name item)
         (has-docs item)) (conj items {} (create-folder base-path item))
    :else items))

(defn- get-index [item]
  (if (= (:name item) "index")
    -1000
    (or (-> item :config :index)
        1000)))

(defn- sort-struct [item]
  (if (seq (:children item))
    (assoc item :children (sort-by (juxt get-index :title)
                                   (map sort-struct (:children item))))
    item))

(defn- filter-struct [item]
  (if (seq (:children item))
    (update item :children #(filter (fn [item]
                                      (some? (:name item)))
                                    %))
    item))

(defn get-struct [path]
  (-> (build-struct [] (file path) path)
      last
      filter-struct
      sort-struct
      :children))
