(ns wiki.web.tree
  (:require [clojure.string :refer [escape]]))

(defn escape-url [str]
  (escape str {\% "%25"}))

(defn tree-view [el version]
  (if (contains? el :children)
    (str "<li>"
           "<a href='/" version (escape-url (:url el)) "'><i class='ac ac-folder-open'></i> " (:title el) "</a>"
           "<ul>" (reduce str (map #(tree-view % version) (:children el))) "</ul>"
         "</li>")
    (str "<li> <a href='/" version (escape-url (:url el)) "'><i class='ac ac-file-text'></i> " (:title el) "</a></li>")))


(defn tree-view-local [el version path]
  (let [local-path (str path (subs (:url el) 1))]
    (if (contains? el :children)
      (str "<li>"
           "<a href='" local-path ".html'><i class='ac ac-folder'></i> " (:title el) "</a>"
           "<ul>" (reduce str (map #(tree-view-local % version path) (:children el))) "</ul>"
           "</li>")
      (str "<li> <a href='" local-path ".html'><i class='ac ac-file-text'></i> " (:title el) "</a></li>"))))