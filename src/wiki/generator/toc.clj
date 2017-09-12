(ns wiki.generator.toc
  (:require [clojure.string :as string])
  (import [org.jsoup Jsoup]))


(defn li [h]
  (str "<li><a href='#"
       (.attr h "id")
       "'>"
       (.text h)
       "</a>"))


(defn menu-from-hs [hs]
  ;(println hs)
  (let [*res (volatile! "")
        *cur-tag (volatile! "")
        *ul-count (volatile! 0)]
    (doseq [h hs]
      ;(println "Iter: " (.tagName h) @*cur-tag)
      (case (Integer/signum (compare (.tagName h) @*cur-tag))
        1 (do
            (vswap! *res str "<ul>")
            (vswap! *ul-count inc))
        -1 (do
             (vswap! *res str "</li></ul>")
             (vswap! *ul-count dec))
        0 (vswap! *res str "</li>"))
      (vreset! *cur-tag (.tagName h))
      (vswap! *res #(str % (li h))))
    (vreset! *res (str @*res (apply str (repeat @*ul-count "</li></ul>"))))))


(defn drop-hand-written-toc [html]
  (let [doc (Jsoup/parseBodyFragment html)
        ul (.select doc "ul")
        hs (.select doc "h1,h2,h3,h4,h5,h6")]
    ;(println "hs: " hs)
    (when (string/starts-with? html "<ul>")
      (.remove (first ul)))
    (.prettyPrint (.outputSettings doc) false)
    (str (menu-from-hs hs) (.html (.body doc)))))


(defn add-toc [html]
  (let [h1-index (+ 5 (string/index-of html "</h1>"))
        h1 (subs html 0 h1-index)
        content (string/trim (subs html h1-index))]
    (str h1 (drop-hand-written-toc content))))