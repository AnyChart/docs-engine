(ns wiki.data.search
  (:require [wiki.components.sphinx :as sphinx]))

(defn search-for [jdbc query version]
  (sphinx/query jdbc "select url, snippet(content, ?) from docs_stg_index where match(?) and version_id=? limit 100;" [query query version]))
