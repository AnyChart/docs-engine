(ns wiki.generator.tree)

(declare generate-tree-item)

(defn- generate-page-item [base-url item]
  {:name (:name item)
   :title (:title item)
   :url (str base-url "/" (:name item))})

(defn- generate-folder-item [base-url item]
  {:name (:name item)
   :title (:title item)
   :url (str base-url "/" (:name item))
   :children (map #(generate-tree-item (str base-url "/" (:name item))
                                       %)
                  (:children item))})

(defn- generate-tree-item [base-url item]
  (if (:content item)
    (generate-page-item base-url item)
    (generate-folder-item base-url item)))

(defn generate-tree [data]
  (map #(generate-tree-item "" %) data))
