(ns wiki.generator.api-versions
  (:require [cheshire.core :refer [parse-string]]
            [org.httpkit.client :as http]))

(defn get-versions [url]
  (-> @(http/get url)
      :body
      parse-string))
