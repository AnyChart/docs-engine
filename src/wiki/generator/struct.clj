(ns wiki.generator.struct
  (:require [clojure.java.io :refer [file]]))

(defn- title [f]
  (-> f .getName
      (clojure.string/replace #"_" " ")
      (clojure.string/replace #"\.md$" "")))

(defn- is-doc [f]
  (re-matches #".*\.md$" (-> f .getName .toLowerCase)))

(defn- get-name [f]
  (-> f .getName
      (clojure.string/replace #"\.md$" "")))

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
      (clojure.string/replace-first #"\A(?s)(?m)(^\{[^\}]+\})" "")
      (clojure.string/trim-newline)))

(defn- create-document [item]
  (let [content (slurp item)
        doc-header (re-matches #"(?s)(?m)(^\{[^\}]+\}).*" content)
        res {:name (get-name item)
             :title (title item)
             :content content
             :config {:index 1000}}]
    (if doc-header
      (-> res
          (update :config #(merge % (-> doc-header last read-string)))
          (update :content fix-document-content))
      res)))

(declare build-struct)

(defn- create-folder [item]
  (let [res {:name (get-name item)
             :title (title item)
             :config {:index 1000}
             :children (reduce build-struct []
                               (.listFiles item))}]
    (if (.exists (file (str (.getAbsolutePath item) "/group.cfg")))
      (update res :config #(merge % (-> (str (.getAbsolutePath item) "/group.cfg")
                                        slurp
                                        read-string)))
      res)))

(defn- build-struct [items item]
  (cond
    (.isHidden item) items
    (and (not (.isDirectory item))
         (is-doc item)) (conj items (create-document item))
    (and (.isDirectory item)
         (has-docs item)) (conj items {} (create-folder item))
    :else items))

(defn- get-index [item]
  (if (= (:name item) "index")
    -1000
    (or (-> item :config :index)
        1000)))

(defn- sort-struct [item]
  (if (seq (:children item))
    (update item :children #(sort-by (juxt (fn [i] (get-index i))
                                           :title)
                                     (map sort-struct %)))
    item))

(defn- filter-struct [item]
  (if (seq (:children item))
    (update item :children #(filter (fn [item]
                                      (some? (:name item)))
                                    %))
    item))

(defn get-struct [path]
  (-> (build-struct [] (file path))
      last
      filter-struct
      sort-struct
      :children))
