(ns wiki.data.utils
  (:require [cheshire.core :refer [generate-string parse-string]])
  (:import (org.postgresql.util PGobject)))

(defn clj->jsonb
  "Converts the given value to a PG JSONB object"
  [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(defn pg->clj [pg-obj]
  (parse-string (.getValue pg-obj) true))