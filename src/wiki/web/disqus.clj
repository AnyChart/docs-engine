(ns wiki.web.disqus)

(defn get-map [url]
  (let [new-url (-> url
                    (clojure.string/replace #"localhost:8080" "docs.anychart.com")
                    (clojure.string/replace #"127.0.0.1" "docs.anychart.com")
                    (clojure.string/replace #"\.stg" ".com")
                    ;; delete version, be careful with already deleted version url
                    (clojure.string/replace #"\.com/[^/]+" ".com"))]
    (str url ", " new-url)))

;; migrate disqus urls from .stg to .com, generate csv file, https://anychart-docs.disqus.com/admin/discussions/migrate/
(defn get-mapping [in-path out-path]
  (with-open [rdr (clojure.java.io/reader in-path)]
    (let [lines (line-seq rdr)
          new-lines (map get-map lines)
          result (clojure.string/join "\n" new-lines)]
      (spit out-path result))))