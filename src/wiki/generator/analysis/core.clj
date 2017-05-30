(ns wiki.generator.analysis.core
  (:require [clojure.string :as s]
            [link-checker.core :as link-checker]
            [link-checker.url]
            [wiki.data.versions :as vdata]))

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


(defn check-broken-links [jdbc version report domain *broken-link-result]
  (let [sitemap-url (case domain
                      :stg (str "http://docs.anychart.stg/sitemap/" (:key version))
                      :prod (str "https://docs.anychart.com/sitemap/" (:key version))
                      :local (str "http://localhost:8080/sitemap/" (:key version)))
        sitemap-urls (map link-checker.url/prepare-url (link-checker/urls-from-sitemap sitemap-url))
        sitemap-urls (map
                       (fn [s] (case domain
                                 :prod s
                                 :stg (-> s
                                          (clojure.string/replace #"\.com" ".stg")
                                          (clojure.string/replace #"https:" "http:")
                                          )
                                 :local (-> s (clojure.string/replace #"https://docs\.anychart\.com" "http://localhost:8080"))))
                       sitemap-urls)
        config {:check-fn       (fn [url data]
                                  (case domain
                                    :stg (.contains url (str "//docs.anychart.stg/" (:key version) "/"))
                                    :prod (.contains url (str "//docs.anychart.com/" (:key version) "/"))
                                    :local (.contains url (str "//localhost:8080/" (:key version) "/"))))
                :max-loop-count 100
                :end-fn         (fn [res]
                                  ;(prn "LINK CHEKER RESULT: " res)
                                  (let [total-report {:error-links  report
                                                      :broken-links res}]
                                    (deliver *broken-link-result total-report)))}]
    (link-checker/start-by-urls sitemap-urls sitemap-url config)))


(defn format-report [broken-links]
  (let [broken-links (mapcat (fn [link]
                               (map (fn [from-link] (assoc from-link :bad-url (:url link)))
                                    (:from link)))
                             broken-links)]
    broken-links))