(ns wiki.web.tree
  (:require [clojure.string :refer [escape]]
            [wiki.util.utils :as utils]))


(defn tree-view [el version is-url-version]
  (let [url (if is-url-version
              (str "/" version (utils/escape-url (:url el)))
              (utils/escape-url (:url el)))]
    (if (contains? el :children)
      (str "<li>"
           "<a href='" url "'><i class='ac ac-folder-open'></i> " (:title el) "</a>"
           "<ul>" (reduce str (map #(tree-view % version is-url-version) (:children el))) "</ul>"
           "</li>")
      (str "<li> <a href='" url "'><i class='ac ac-file-text'></i> " (:title el) "</a></li>"))))


(defn tree-view-local [el version path]
  (let [local-path (str path (subs (:url el) 1))]
    (if (contains? el :children)
      (str "<li>"
           "<a href='" local-path ".html'><i class='ac ac-folder'></i> " (:title el) "</a>"
           "<ul>" (reduce str (map #(tree-view-local % version path) (:children el))) "</ul>"
           "</li>")
      (str "<li> <a href='" local-path ".html'><i class='ac ac-file-text'></i> " (:title el) "</a></li>"))))