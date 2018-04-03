(ns wiki.config.core
  (:require [wiki.config.spec :as config-spec]
            [clojure.spec.alpha :as s]))


(defonce common nil)

(defn set-config [conf] (alter-var-root (var common) (constantly (:common conf))))


(defn prefix [] (:prefix common))
(defn domain [] (:domain common))
(defn reference [] (:reference common))
(defn playground [] (:playground common))



(defn check-config [data]
  (s/valid? ::config-spec/config data))

(defn explain-config [data]
  (s/explain-str ::config-spec/config data))