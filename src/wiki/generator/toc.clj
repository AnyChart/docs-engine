(ns wiki.generator.toc
  (:require [clojure.string :as string]
            [taoensso.timbre :as timbre])
  (:import (org.jsoup Jsoup)))


(defn li [h]
  (str "<li><a href='#"
       (.attr h "id")
       "'>"
       (.text h)
       "</a>"))


(defn menu-from-hs [hs *page-report page-url]
  (let [*error (volatile! false)
        *res (volatile! "")
        *cur-tag (volatile! "h1")
        *ul-count (volatile! 0)]
    (when (and
            (seq hs)
            (not= (.tagName (first hs)) "h2"))
      (vreset! *error true)
      (swap! *page-report (fn [page-report] (update page-report :toc-error conj
                                                    (str (.tagName (first hs)) " - " (.text (first hs)))))))
    (doseq [h hs]
      (let [diff (compare (.tagName h) @*cur-tag)]
        (cond
          (> diff 0) (do
                       (vswap! *res str "<ul>")
                       (vswap! *ul-count inc)
                       (when (> diff 1)
                         (vreset! *error true)
                         (swap! *page-report (fn [page-report] (update page-report :toc-error conj
                                                                       (str (.tagName h) " - " (.text h)))))))
          (< diff 0) (dotimes [_ (Math/abs diff)]
                       (vswap! *res str "</li></ul>")
                       (vswap! *ul-count dec))
          (= diff 0) (vswap! *res str "</li>")))
      (vreset! *cur-tag (.tagName h))
      (vswap! *res #(str % (li h))))
    (vreset! *res (str @*res (apply str (repeat @*ul-count "</li></ul>"))))
    (when @*error (timbre/error "TOC error: " page-url))
    @*res))


(defn drop-hand-written-toc [html *page-report page-url]
  (let [doc (Jsoup/parseBodyFragment html)
        ul (.select doc "ul")
        hs (.select doc "h1,h2,h3,h4,h5,h6")]
    ;(println "hs: " hs)
    (when (string/starts-with? html "<ul>")
      (.remove (first ul)))
    (.prettyPrint (.outputSettings doc) false)
    (str (menu-from-hs hs *page-report page-url) (.html (.body doc)))))


(defn add-toc [html *page-report page-url]
  (if-let [h1-index (string/index-of html "</h1>")]
    (let [h1-index (+ 5 h1-index)
          h1 (subs html 0 h1-index)
          content (string/trim (subs html h1-index))]
      (str h1 (drop-hand-written-toc content *page-report page-url)))
    (do
      (timbre/error "TOC error - h1 not found: " page-url)
      (swap! *page-report (fn [page-report]
                            (update page-report :toc-error conj
                                    "h1 - not found")))
      html)))