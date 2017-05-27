(ns wiki.generator.analysis.core
  (:require [clojure.string :as s]))

(defn landing-link? [link]
  (or (.endsWith link ".com")
      (.endsWith link ".com/")))

(defn anchor-link? [link]
  (.startsWith link "#"))

(defn check-canonical [link]
  (when (and
          (not (.contains link ".com"))
          (or (re-find #"/\d+\.\d+\.\d+" link)
              (re-find #"/latest" link)))
    {:canonical-error true}))

(defn check-https [link]
  (when (.startsWith link "http:")
    {:https-error true}))

(defn check-direct [link]
  (when (re-find #"(api.anychart|doc.anychart|pg.anychart|playground.anychart)" link)
    {:direct-error true}))

(defn check-env [link]
  (when (re-find #"(stg|develop/localhost|8080)" link)
    {:env-error true}))

(defn get-links [md]
  (let [links-list (re-seq #"\[[^\[\]]*\]\(([^\)]*)\)" md)
        links (map second links-list)]
    links))

(defn check-links [md]
  (let [all-links (get-links md)
        links (remove #(or (landing-link? %)
                           (anchor-link? %)) all-links)
        canonical-links (filter check-canonical links)
        https-links (filter check-https all-links)
        direct-links (filter check-direct links)
        env-links (filter check-env links)]
    (reduce (fn [res [k coll]]
              (if (seq coll)
                (assoc res k coll)
                res)) {} [[:http-links https-links]
                          [:non-canonical-links canonical-links]
                          [:direct-links direct-links]
                          [:env-links env-links]])))


(defn start [md]
  (check-links md))



