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
          (not-any? #(.contains link %) [".com" ".org" ".ru" ".neta"])
          (or (re-find #"/\d+\.\d+\.\d+" link)
              (re-find #"/latest" link)
              (and (.startsWith link "/")
                   (not (.startsWith link "//")))))
    {:canonical-error true}))

(defn check-https [link]
  (when (.startsWith link "http:")
    {:https-error true}))

(defn check-direct [link]
  (when (or
          (and (re-find #"(api.anychart|docs.anychart|pg.anychart|playground.anychart)" link)
               (re-find #"/\d+\.\d+\.\d+" link))
          (and (re-find #"docs.anychart" link)
               (or (re-find #"/\d+\.\d+\.\d+" link)
                   (.contains link "/latest"))))
    {:direct-error true}))

(defn check-env [link]
  (when (re-find #"(stg|develop/localhost|8080)" link)
    {:env-error true}))

(defn get-links [md]
  (let [links-list (re-seq #"\[[^\[\]]*\]\(([^\)]*)\)" md)
        links (map second links-list)]
    links))

(defn check-links [md version-config]
  (let [all-links (get-links md)
        links (remove #(or (landing-link? %)
                           (anchor-link? %)
                           (some (fn [http-ignore]
                                   (.contains % http-ignore))
                                 (-> version-config :report :http-ignore))) all-links)
        canonical-links (filter check-canonical links)
        https-links (filter check-https
                            (remove #(some (fn [http-ignore]
                                             (.contains % http-ignore))
                                           (-> version-config :report :http-ignore)) all-links))
        direct-links (filter check-direct links)
        env-links (filter check-env links)]
    (reduce (fn [res [k coll]]
              (if (seq coll)
                (assoc res k coll)
                res)) {} [[:http-links https-links]
                          [:non-canonical-links canonical-links]
                          [:direct-links direct-links]
                          [:env-links env-links]])))


;; =====================================================================================================================
;; Check version links
;; =====================================================================================================================

(defn domain-url [domain]
  (case domain
    :stg "http://docs.anychart.stg/"
    :prod "https://docs.anychart.com/"
    :local "http://localhost:8080/"))


;(defn get-check-fn [domain version docs-versions]
;  (fn [url data]
;    (and (not (.contains url "export-server.jar"))
;         (not (.endsWith url (str "/" (:key version) "/download")))
;         (or (.contains url (str (domain-url domain) (:key version) "/"))
;             (and
;               (or (and (.contains url "anychart.stg/")
;                        (.contains url (:key version)))
;                   (and (.contains url "anychart.com/")
;                        (.contains url (:key version)))
;                   (.contains url "//anychart.")
;                   (and (or (.contains url "docs.anychart.")
;                            (.contains url "localhost:8080"))
;                        (not-any? (fn [version-name] (.contains url version-name)) docs-versions)))
;               (seq (filter (fn [from-link]
;                              (.contains
;                                (:url from-link)
;                                (str (domain-url domain) (:key version) "/")))
;                            (:from data))))))))


;(defn check-broken-links [jdbc version docs-versions report domain *broken-link-result]
;  (let [sitemap-url (str (domain-url domain) "sitemap/" (:key version))
;        sitemap-urls (map link-checker.url/prepare-url (link-checker/urls-from-sitemap sitemap-url))
;        sitemap-urls (map
;                       (fn [s] (case domain
;                                 :prod s
;                                 :stg (-> s
;                                          (clojure.string/replace #"\.com" ".stg")
;                                          (clojure.string/replace #"https:" "http:"))
;                                 :local (-> s (clojure.string/replace #"https://docs\.anychart\.com" "http://localhost:8080"))))
;                       sitemap-urls)
;        config {:check-fn         (get-check-fn domain version docs-versions)
;                :iteration-fn     (fn [iteration urls-count urls-for-check-total-count total-count]
;                                    (println "Iteration: " iteration urls-count urls-for-check-total-count total-count))
;                :max-loop-count   30
;                :default-protocol "http"
;                :end-fn           (fn [res]
;                                    (let [total-report {:error-links  report
;                                                        :broken-links (link-checker-utils/revert-result res)}]
;                                      (deliver *broken-link-result total-report)))}]
;    (link-checker/start-by-urls sitemap-urls sitemap-url config)))


;; =====================================================================================================================
;; Check version links | NEW VERSION
;; =====================================================================================================================
(defn get-check-fn [version-key domain]
  (fn [url]
    (and (not (.contains url "export-server.jar"))
         (not (.endsWith url (str "/" version-key "/download")))
         (.contains url (str (domain-url domain) version-key "/")))))

(defn get-add-fn [version-key domain]
  (fn [url]
    (cond
      ;; cause it's not ready when it checks
      (.endsWith url (str "/" version-key "/download")) false
      ;; cause it's banned in Russia
      (= url "https://www.linkedin.com/company/386660") false
      ;; cause github's anchor without id="overview"
      (= url "https://github.com/AnyChart/docs.anychart.com#overview") false
      ;; allow only current version urls for deep analysis
      (.startsWith url (str (domain-url domain) version-key "/")) true
      (.startsWith url (domain-url domain)) false
      :else true)))


(defn get-sitemap-urls [version-key domain]
  (let [sitemap-url (str (domain-url domain) "sitemap/" version-key)
        sitemap-urls (map link-checker.url/prepare-url (link-checker/urls-from-sitemap sitemap-url))
        sitemap-urls (map
                       (fn [s] (case domain
                                 :prod s
                                 :stg (-> s
                                          (clojure.string/replace #"\.com" ".stg")
                                          (clojure.string/replace #"https:" "http:"))
                                 :local (-> s (clojure.string/replace #"https://docs\.anychart\.com" "http://localhost:8080"))))
                       sitemap-urls)]
    sitemap-urls))

(defn check-broken-links [version-key report domain *broken-link-result]
  (let [sitemap-url (str (domain-url domain) "sitemap/" version-key)
        sitemap-urls (get-sitemap-urls version-key domain)
        config {:check-fn         (get-check-fn version-key domain)
                :add-fn           (get-add-fn version-key domain)
                :iteration-fn     (fn [iteration urls-count urls-for-check-total-count total-count]
                                    (println "Iteration: " iteration urls-count urls-for-check-total-count total-count))
                :max-loop-count   40
                :default-protocol "http"
                :end-fn           (fn [res]
                                    (let [total-report {:error-links  report
                                                        :broken-links (link-checker-utils/revert-result res)}]
                                      (deliver *broken-link-result total-report)))}]
    (link-checker/start-by-urls sitemap-urls sitemap-url config)))