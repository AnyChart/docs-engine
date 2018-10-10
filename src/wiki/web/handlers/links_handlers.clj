(ns wiki.web.handlers.links-handlers
  (:require [wiki.web.helpers :refer :all]
            [ring.util.response :refer [response]]
            [wiki.data.pages :as pages-data]
            [wiki.util.utils :as utils]))


(defn has-intersection [v1 v2]
  (some (set v1) v2))


(defn links [request]
  (prn "Links request: " request)
  (let [version (-> request :params :version)
        project (-> request :params :project)
        url (-> request :params :url)
        api-methods (-> request :params :api-methods)

        ;version "develop"
        ;project "docs"
        ;url "samples/quick_start_pie"
        ;;api-methods ["anychart#onDocumentReady" "anychart.data#loadJsonFile" "anychart.standalones#table" "anychart.core.ui.Table#hAlign" "anychart.core.ui.Table#contents" "anychart.core.ui.Table#getRow" "anychart.core.ui.table.Row#height" "anychart.charts.Resource#cellPadding" "anychart.core.ui.table.Row#cellPadding" "anychart.core.ui.Table#getCell" "anychart.core.ui.table.Cell#colSpan" "anychart.core.Text#useHtml" "anychart.core.ui.Table#cellBorder" "anychart.standalones.Table#container" "anychart.core.ui.Table#vAlign" "anychart.core.ui.Table#draw" "anychart#bullet" "anychart.core.Chart#background" "anychart.core.VisualBase#enabled" "anychart.core.Chart#padding" "anychart.charts.Bullet#layout" "anychart.charts.Bullet#axis" "anychart.core.Chart#title"]
        ;api-methods ["anychart#onDocumentLoad" "anychart#pie" "anychart.charts.Pie#data" "anychart.core.Chart#title" "anychart.core.Chart#container" "anychart.core.Chart#draw"]

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

        articles-pg (filter (fn [page]
                              (let [pg (-> page :config :links :pg)]
                                ;; check on pg links
                                (some #(and (= project (:project %))
                                            (= url (:url %))) pg)))
                            pages)

        articles-fn (fn [pages]
                      (map (fn [page]
                             {:title (utils/url->title (:url page))
                              :url   (:url page)})
                           pages))

        articles-docs (articles-fn articles-docs)
        articles-api (articles-fn articles-api)
        articles-pg (articles-fn articles-pg)

        result {:articles-docs articles-docs
                :articles-api  articles-api
                :articles-pg   articles-pg}]

    (prn "Links request:")
    (prn "Version " version)
    (prn "Project " project)
    (prn "URL " url)
    (prn "api-methods " api-methods)

    (response result)))