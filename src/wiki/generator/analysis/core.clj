(ns wiki.generator.analysis.core
  (:require [clojure.string :as s]
            [link-checker.core :as link-checker]
            [link-checker.url]
            [link-checker.utils :as link-checker-utils]
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


(defn domain-url [domain]
  (case domain
    :stg "http://docs.anychart.stg/"
    :prod "https://docs.anychart.com/"
    :local "http://localhost:8080/"))


(defn get-check-fn [domain version]
  (fn [url data]
    (and (not (.contains url "export-server.jar"))
         (or (.contains url (str (domain-url domain) (:key version) "/"))
             (and
               (or (and (.contains url "anychart.stg/")
                        (.contains url (:key version)))
                   (and (.contains url "anychart.com/")
                        (.contains url (:key version))))
               (seq (filter (fn [from-link]
                              (.contains
                                (:url from-link)
                                (str (domain-url domain) (:key version) "/")))
                            (:from data))))))))


(defn check-broken-links [jdbc version report domain *broken-link-result]
  (let [sitemap-url (str (domain-url domain) "sitemap/" (:key version))
        sitemap-urls (map link-checker.url/prepare-url (link-checker/urls-from-sitemap sitemap-url))
        sitemap-urls (map
                       (fn [s] (case domain
                                 :prod s
                                 :stg (-> s
                                          (clojure.string/replace #"\.com" ".stg")
                                          (clojure.string/replace #"https:" "http:"))
                                 :local (-> s (clojure.string/replace #"https://docs\.anychart\.com" "http://localhost:8080"))))
                       sitemap-urls)
        config {:check-fn         (get-check-fn domain version)
                :iteration-fn     (fn [iteration urls-count urls-for-check-total-count total-count]
                                    (println "Iteration: " iteration urls-count urls-for-check-total-count total-count))
                :max-loop-count   25
                :default-protocol "http"
                :end-fn           (fn [res]
                                    (let [total-report {:error-links  report
                                                        :broken-links (link-checker-utils/revert-result res)}]
                                      (deliver *broken-link-result total-report)))}]
    (link-checker/start-by-urls sitemap-urls sitemap-url config)))