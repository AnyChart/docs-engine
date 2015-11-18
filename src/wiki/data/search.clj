(ns wiki.data.search
  (:require [wiki.components.sphinx :as sphinx]
            [ring.util.response :refer [content-type response]]))

(defn search-for [jdbc query version-id version-key]
  (sphinx/query jdbc "select url,snippet(content, ?) as sn from docs_stg_index where match(?) and version_id <> ? limit 100;" [query query version-id]))
