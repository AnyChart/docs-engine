(ns wiki.generator.analysis.core
  (:require [wiki.config.core :as c]
            [clojure.string :as s]
            [link-checker.core :as link-checker]
            [link-checker.url]
            [link-checker.utils :as link-checker-utils]
            [taoensso.timbre :as timbre]))


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
(defn get-check-fn [version-key]
  (fn [url]
    (and (not (.contains url "export-server.jar"))
         (not (.endsWith url (str "/" version-key "/download")))
         (.contains url (str (c/domain) version-key "/")))))


(defn get-add-fn [version-key]
  (fn [url]
    (cond
      ;; cause it's not ready when it checks
      (.endsWith url (str "/" version-key "/download")) false
      ;; cause it's banned in Russia
      (= url "https://www.linkedin.com/company/386660") false
      ;; cause github's anchor without id="overview"
      (= url "https://github.com/AnyChart/docs.anychart.com#overview") false
      ;; allow only current version urls for deep analysis
      (.startsWith url (str (c/domain) version-key "/")) true
      (.startsWith url (c/domain)) false
      :else true)))


(defn get-sitemap-urls [version-key]
  (let [sitemap-url (str (c/domain) "sitemap/" version-key)
        sitemap-urls (link-checker/urls-from-sitemap sitemap-url)]
    sitemap-urls))


(defn check-broken-links [version-key report *broken-link-result]
  (timbre/info "Start check-broken-links")
  (let [sitemap-url (str (c/domain) "sitemap/" version-key)
        sitemap-urls (get-sitemap-urls version-key)
        config {:check-fn         (get-check-fn version-key)
                :add-fn           (get-add-fn version-key)
                :iteration-fn     (fn [iteration urls-count urls-for-check-total-count total-count]
                                    (println "Iteration: " iteration urls-count urls-for-check-total-count total-count))
                :max-loop-count   45
                :default-protocol "http"
                :end-fn           (fn [res]
                                    (let [total-report {:error-links  report
                                                        :broken-links (link-checker-utils/revert-result res)}]
                                      (deliver *broken-link-result total-report)))}]
    (link-checker/start-by-urls sitemap-urls sitemap-url config)))