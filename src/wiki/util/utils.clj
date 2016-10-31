(ns wiki.util.utils)

(defn released-version? [version-key]
  (re-matches #"^\d+\.\d+\.\d+$" version-key))

(defn anychart-bundle-path [version-key]
  (if (released-version? version-key)
    (str "https://cdn.anychart.com/js/" version-key "/anychart-bundle.min.js")
    (str "http://static.anychart.com/js/" version-key "/anychart-bundle.min.js")))

(defn anychart-bundle-css-url [version-key]
  (if (released-version? version-key)
    (str "https://cdn.anychart.com/css/" version-key "/anychart-ui.min.css")
    (str "http://static.anychart.com/css/" version-key "/anychart-ui.min.css")))