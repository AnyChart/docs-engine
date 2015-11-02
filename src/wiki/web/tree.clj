(ns wiki.web.tree)

(defn tree-view [el version]
  (if (contains? el :children)
    (str "<li>"
           "<a href='/" version (:url el) "'><i class='fa fa-folder-open'></i> " (:title el) "</a>"
           "<ul>" (reduce str (map #(tree-view % version) (:children el))) "</ul>"
         "</li>")
    (str "<li> <a href='/" version (:url el) "'>" (:title el) "</a></li>")))
