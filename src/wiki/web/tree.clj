(ns wiki.web.tree)

(defn tree-view [el version]
  (if (contains? el :children)
    (str "<li>"
           "<a href='/" version (:url el) "'><i class='fa fa-folder-open'></i> " (:title el) "</a>"
           "<ul>" (reduce str (map #(tree-view % version) (:children el))) "</ul>"
         "</li>")
    (str "<li> <a href='/" version (:url el) "'><i class='fa fa-file'></i> " (:title el) "</a></li>")))


(defn tree-view-local [el version path]
  (let [local-path (str path (subs (:url el) 1))]
    (if (contains? el :children)
      (str "<li>"
           "<a href='" local-path ".html'><i class='fa fa-folder'></i> " (:title el) "</a>"
           "<ul>" (reduce str (map #(tree-view-local % version path) (:children el))) "</ul>"
           "</li>")
      (str "<li> <a href='" local-path ".html'><i class='fa fa-file'></i> " (:title el) "</a></li>"))))
