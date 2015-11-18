(ns wiki.web.routes
  (:require [selmer.parser :refer [render-file add-tag!]]
            [compojure.core :refer [defroutes routes GET POST]]
            [compojure.route :as route]
            [ring.util.response :refer [redirect response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response]]
            [wiki.components.redis :as redisca]
            [wiki.components.notifier :refer [notify-404]]
            [wiki.data.versions :as versions-data]
            [wiki.data.pages :as pages-data]
            [wiki.data.folders :as folders-data]
            [wiki.data.search :as search]
            [wiki.web.tree :refer [tree-view]]))

(add-tag! :tree-view (fn [args context-map]
                       (let [entries (get context-map (keyword (first args)))]
                         (reduce str (map #(tree-view % (:version context-map)) entries)))))

(defn- jdbc [request]
  (-> request :component :jdbc))

(defn- sphinx [request]
  (-> request :component :sphinx))

(defn- redis [request]
  (-> request :component :redis))

(defn- notifier [request]
  (-> request :component :notifier))

(defn- show-404 [request]
  (render-file "templates/404.selmer" {}))

(defn- error-404 [request]
  (let [referrer (get-in request [:headers "referer"])
        agent (get-in request [:headers "http_user_agent"])]
    (when (not (.contains agent "Slackbot"))
      (if referrer
        (notify-404 (notifier request) (str (request-url request) " from " referrer))
        (notify-404 (notifier request) (request-url request)))))
  (route/not-found (show-404 request)))

(defn- request-update [request]
  (redisca/enqueue (redis request)
                   (-> request :component :config :queue)
                   "generate"))

(defn- show-landing [request]
  (redirect (str "/" (versions-data/default (jdbc request)) "/Quick_Start")))

(defn- show-latest [request]
  (redirect (str "/" (versions-data/default (jdbc request)) "/Quick_Start")))

(defn- show-latest-search [request]
  (redirect (str "/" (versions-data/default (jdbc request)) "/search?q=" (-> request :params :q))))

(defn- show-version [request version]
  (redirect (str "/" (:key version) "/Quick_Start")))

(defn- try-show-page [request version]
  (let [url (-> request :route-params :*)]
    (if (pages-data/page-exists? (jdbc request) (:id version))
      (redirect (str "/" (:key version) "/" url))
      (redirect (str "/" (:key version) "/Quick_Start")))))

(defn- show-page-data [request version page]
  (response {:url (:url page)
             :page page}))

(defn- show-page [request version page]
  (render-file "templates/page.selmer" {:version (:key version)
                                        :tree (versions-data/tree-data (jdbc request)
                                                                       (:id version))
                                        :url (:url page)
                                        :page page
                                        :versions (versions-data/versions (jdbc request))}))

(defn- try-show-latest-page [request]
  (let [version (versions-data/default (jdbc request))]
    (redirect (str "/" version "/" (-> request :route-params :*)))))

(defn- search-results [request version]
  (render-file "templates/search.selmer" {:version (:key version)
                                          :tree (versions-data/tree-data (jdbc request)
                                                                         (:id version))
                                          :query (-> request :params :q)
                                          :versions (versions-data/versions (jdbc request))
                                          :results (search/search-for (sphinx request)
                                                                      (-> request :params :q)
                                                                      (:id version)
                                                                      (:key version))}))

(defn- search-data [request version]
  (response (search/search-for (sphinx request)
                               (-> request :params :q)
                               (:id version)
                               (:key version))))

(defn- check-version-middleware [app]
  (fn [request]
    (if-let [version (versions-data/version-by-key (jdbc request)
                                                   (-> request :route-params :version))]
      (app request version)
      (error-404 request))))

(defn- check-folder-middleware [app]
  (fn [request]
    (let [version (versions-data/version-by-key (jdbc request)
                                                (-> request :route-params :version))
          folder (folders-data/get-folder-by-url (jdbc request)
                                                 (:id version)
                                                 (-> request
                                                     :route-params
                                                     :*))]
      (if folder
        (redirect (str "/" (:key version) "/" (:url folder)
                       "/" (:default_page folder)))
        (if-let [page (pages-data/page-by-url (jdbc request) (:id version)
                                              (-> request :route-params :*))]
          (app request version page)
          (error-404 request))))))

(defn- check-page-middleware [app]
  (fn [request]
    (let [version (versions-data/version-by-key (jdbc request)
                                                (-> request :route-params :version))
          page (pages-data/page-by-url (jdbc request) (:id version)
                                       (-> request :route-params :*))]
      (if (and version page)
        (app request version page)
        (error-404 request)))))

(defroutes app-routes
  (route/resources "/")
  (GET "/_update_" [] request-update)
  (POST "/_update_" [] request-update)
  (GET "/" [] show-landing)
  (GET "/latest" [] show-latest)
  (GET "/latest/" [] show-latest)
  (GET "/latest/search" [] show-latest-search)
  (GET "/latest/*" [] try-show-latest-page)
  (GET "/:version" [] (check-version-middleware show-version))
  (GET "/:version/" [] (check-version-middleware show-version))
  (GET "/:version/check/*" [] (check-version-middleware try-show-page))
  (GET "/:version/*-json" [] (check-page-middleware show-page-data))
  (GET "/:version/search" [] (check-version-middleware search-results))
  (POST "/:version/search-data" [] (check-version-middleware search-data))
  (GET "/:version/*" [] (check-folder-middleware show-page))
  (route/not-found show-404))

(def app (-> (routes app-routes)
             wrap-keyword-params
             wrap-params))
