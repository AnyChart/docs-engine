(ns wiki.web.handlers.links-handlers
  (:require [wiki.web.helpers :refer :all]
            [ring.util.response :refer [response]]
            [wiki.data.pages :as pages-data]
            [wiki.util.utils :as utils]
            [wiki.data.versions :as versions-data]
            [taoensso.timbre :as timbre]))


(defn has-intersection [v1 v2]
  (some (set v1) v2))


(defn links [request]
  (let [version (-> request :params :version)
        version (if (or (= version "latest")
                        (= version "Release Candidate")
                        (= version "rc"))
                  (versions-data/default (jdbc request))
                  version)

        project (-> request :params :project)
        url (-> request :params :url)
        api-methods (-> request :params :api-methods)

        ;version "develop"
        ;project "docs"
        ;; url "samples/quick_start_pie"
        ;url "samples/BCT_Area_Chart_02"
        ;api-methods ["anychart#onDocumentReady" "anychart.data#set" "anychart.data.Set#mapAs" "anychart#area" "anychart.charts.Cartesian#interactivity" "anychart.core.utils.Interactivity#hoverMode" "anychart.charts.Cartesian#area" "anychart.core.cartesian.series.Base#name" "anychart.core.cartesian.series.Base#normal" "anychart.core.cartesian.series.Base#hovered" "anychart.core.cartesian.series.Base#selected" "anychart.charts.Venn#hatchFill" "anychart.charts.Cartesian#title" "anychart.charts.Cartesian#xAxis" "anychart.charts.Cartesian#yAxis" "anychart.charts.Cartesian#container" "anychart.charts.Cartesian#draw"]

        pages (pages-data/links (jdbc request) version)

        articles-docs (filter (fn [page]
                                (let [samples (-> page :config :links :samples)]
                                  ;; check page samples
                                  (some #(and (= project "docs")
                                              (= url (str "samples/" (:path %)))) samples)))
                              pages)

        articles-api (filter (fn [page]
                               (let [api-urls (-> page :config :links :api)]
                                 ;; check on API methods
                                 (has-intersection (map :url api-urls) api-methods)))
                             pages)
        ;; remove links that already are in articles-docs
        articles-api (remove (fn [page]
                               (prn "Page: " page)
                               (some #(= (:url page) (:url %)) articles-docs))
                             articles-api)


        articles-pg (filter (fn [page]
                              (let [pg (-> page :config :links :pg)]
                                ;; check on pg links
                                (some #(and (= project (:project %))
                                            (= url (:url %))) pg)))
                            pages)

        articles-fn (fn [pages]
                      (->> pages
                           (map (fn [page]
                                  {:title (utils/url->title (:url page))
                                   :url   (:url page)}))
                           (sort-by :title)))

        articles-docs (articles-fn articles-docs)
        articles-api (articles-fn articles-api)
        articles-pg (articles-fn articles-pg)

        result {:articles-docs articles-docs
                :articles-api  articles-api
                :articles-pg   articles-pg}]

    (timbre/info "Links request:")
    (println "Version: " version)
    (println "Project: " project)
    (println "URL: " url)
    (println "api-methods: " api-methods)

    (response result)))