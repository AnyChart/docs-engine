(ns wiki.data.search
  (:require [yesql.core :refer [defqueries]]
            [wiki.components.sphinx :as sphinx]))

(defqueries "sql/search.sql")

(defn search-for [jdbc query version]
  (sql-search-for {:query query
                   :version version}
                  {:connection (sphinx/conn jdbc)}))
