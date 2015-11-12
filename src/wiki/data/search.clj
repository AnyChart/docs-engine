(ns wiki.data.search
  (:require [wiki.components.sphinx :as sphinx]))

(defn search-for [jdbc query version-id version-key]
  (let [res (sphinx/query jdbc "select url,full_name,snippet(content, ?) as sn from docs_stg_index where match(?) and version_id=? limit 100;" [query query version-id])]
    (reduce (fn [res row]
              (let [url (str "/" version-key "/" (:url row))]
                (str res "<h2><a href='" url "'>"
                     (:full_name row) "</a></h2>"
                     (:sn row) "<hr />")))
            "" res)))
