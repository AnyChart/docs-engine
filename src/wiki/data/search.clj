(ns wiki.data.search
  (:require [wiki.components.sphinx :as sphinx]))

(defn search-for [jdbc query version]
  (let [res (sphinx/query jdbc "select url, snippet(content, ?) as sn from docs_stg_index where match(?) and version_id=? limit 100;" [query query version])]
    (reduce (fn [res row]
              (str res "<h1>" (:url row) "</h1>" (:sn row) "<hr />"))
            "" res)))
