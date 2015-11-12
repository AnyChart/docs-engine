(ns wiki.data.search
  (:require [wiki.components.sphinx :as sphinx]
            [ring.util.response :refer [content-type response]]))

(defn search-for [jdbc query version-id version-key]
  (let [res (sphinx/query jdbc "select url,snippet(content, ?) as sn from docs_stg_index where match(?) and version_id=? limit 100;" [query query version-id])]
    (-> (response (reduce (fn [res row]
                            (let [url (str "/" version-key "/" (:url row))]
                              (str res "<strong><a href='" url "'>"
                                   url "</a></strong><br />"
                                   (:sn row) "<hr />")))
                          "" res))
        (content-type "text/html"))))
