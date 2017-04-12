(ns wiki.generator.analysis.core
  (:require [clojure.string :as s]))

(defn check-canonical [link]
  (when (or (re-find #"\d+\.\d+\.\d+" link)
            (re-find #"latest" link))
    {:canonical-error true}))

(defn check-https [link]
  (when (re-find #"http[^s]" link)
    {:https-error true}))

(defn check-direct [link]
  (when (re-find #"(api.anychart|doc.anychart|pg.anychart|playground.anychart)")
    {:direct-error true}))

(defn check-env [link]
  (when (re-find #"(stg|develop|localhost|8080)" link)
    {:env-error true}))

(defn get-links [md]
  (let [links-list (re-seq #"\[[^\[\]]*\]\(([^\)]*)\)" md)
        links (map second links-list)]
    links))

(defn check-links [md]
  (let [links (get-links md)]
    ;(filter some? links)
    ;{:canonical-error (seq (filter))}
    links))


(defn start [path md]
  (merge
    (check-https md)
    (check-links md)))



