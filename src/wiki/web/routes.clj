(ns wiki.web.routes
  (:require
    ;; components
    [wiki.components.redis :as redisca]
    [wiki.components.notifier :refer [notify-404]]
    ;; data
    [wiki.data.versions :as versions-data]
    [wiki.data.pages :as pages-data]
    [wiki.data.folders :as folders-data]
    [wiki.data.sitemap :as sitemap]
    [wiki.data.search :as search]
    [wiki.data.versions :as vdata]
    ;; wiki utils
    [wiki.util.utils :as utils]
    [wiki.generator.analysis.page :as analysis-page]
    ;;web
    [wiki.web.tree :refer [tree-view tree-view-local]]
    [wiki.web.redirects :refer [wrap-redirect]]
    [wiki.web.helpers :refer :all]
    [wiki.web.search :as web-search]
    ;; pages
    [wiki.views.main.main-page :as main-page]
    [wiki.views.page404.page404 :as page-404]
    [wiki.views.admin.admin-page :as admin-view]
    ;; utils
    [selmer.parser :refer [render-file add-tag!]]
    [compojure.core :refer [defroutes routes GET POST]]
    [compojure.route :as route]
    [ring.util.response :refer [redirect response content-type file-response header]]
    [ring.util.request :refer [request-url]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.json :refer [wrap-json-response]]
    [taoensso.timbre :as timbre]
    [criterium.core :refer [bench]]
    [clojure.string :as string]))


(add-tag! :tree-view (fn [args context-map]
                       (let [entries (get context-map (keyword (first args)))]
                         (reduce str (map #(tree-view % (:version context-map) (:is-url-version context-map)) entries)))))


(add-tag! :tree-view-local (fn [args context-map]
                             (let [entries (get context-map (keyword (first args)))
                                   path (get context-map (keyword (second args)))]
                               (reduce str (map #(tree-view-local % (:version context-map) path) entries)))))


(defn- title-prefix [page is-url-version version-name]
  (let [max-chars-count (if is-url-version
                          (- 65 (count (str " | ver. " version-name)))
                          65)
        text (or (-> page :config :title)
                 (-> page :url utils/url->title))
        title (str text " | AnyChart Documentation")
        title-parts (string/split title #" \| ")
        title (reduce (fn [res part]
                        (if (empty? res)
                          part
                          (if (< (count (str res " | " part)) max-chars-count)
                            (str res " | " part)
                            res))) "" title-parts)]
    (str title (when is-url-version (str " | ver. " version-name)))))


(defn- show-404 [request]
  (page-404/page {:title-prefix "Not found | AnyChart Documentation"
                  :description  "404 Not found page"
                  :commit       (:commit (config request))}))


(defn- error-404 [request]
  (let [referrer (get-in request [:headers "referer"])
        ua (get-in request [:headers "user-agent"])]
    (when (not (.contains ua "Slackbot"))
      (if referrer
        (notify-404 (notifier request) (str (request-url request) " from " referrer))
        (notify-404 (notifier request) (request-url request)))))
  (route/not-found (show-404 request)))


(defn- request-update [request]
  (redisca/enqueue (redis request)
                   (-> request :component :config :queue)
                   {:cmd "generate"}))


(defn- show-latest [request]
  (redirect "/Quick_Start"))


(defn- show-latest-search [request]
  (redirect (str "/search?q=" (-> request :params :q))))


(defn- try-show-latest-page [request]
  (redirect (str "/" (-> request :route-params :*))))


(defn- show-version [request version]
  (redirect (str "/" (:key version) "/Quick_Start")))


(defn- try-show-page [request version & [versions url is-url-version]]
  (let [url (-> request :route-params :*)]
    (if (pages-data/page-exists? (jdbc request) (:id version) url)
      (redirect (str "/" (:key version) "/" url))
      (redirect (str "/" (:key version) "/Quick_Start")))))


(defn- old-url [versions url]
  (let [last-version-url (-> versions first :url)]
    (if (.endsWith last-version-url url)
      (str "/" url)
      "/")))


(defn- show-page [request version versions page is-url-version]
  (let [data {:version              (:key version)
              :actual-version       (:key (first versions))
              :is-url-version       is-url-version
              :anychart-url         (utils/anychart-bundle-path (:key version))
              :anychart-css-url     (utils/anychart-bundle-css-url (:key version))
              :old                  (and (not= (:key (first versions)) (:key version))
                                         (utils/released-version? (:key version)))
              :old-url              (old-url versions (:url page))
              :tree                 (versions-data/tree-data (jdbc request) (:id version))
              :url                  (:url page)
              :image-url            (utils/name->url (:url page))
              :title                (:full_name page)
              :title-prefix         (title-prefix page is-url-version (:key version))
              :description          (or (utils/page-description (:content page))
                                        (:full_name page))
              :page                 page
              :versions             versions
              :is-ga-speed-insights (:is-ga-speed-insights request)
              :commit               (:commit (config request))}]
    (main-page/page data)))


(defn- show-landing [request]
  (let [url ""
        versions (versions-data/get-page-versions (jdbc request) url)
        version (first versions)
        ; page (pages-data/page-by-url (jdbc request) (:id version) url)
        ]
    ;if page
    ;(show-page request version versions page false)
    (let [url "Quick_Start/Quick_Start"
          page (pages-data/page-by-url (jdbc request) (:id version) url)]
      (if page
        (show-page request version versions page false)
        (let [folder (folders-data/get-folder-by-url (jdbc request) (:id version) "Quick_Start")
              page (pages-data/page-by-url (jdbc request) (:id version)
                                           (str "Quick_Start/" (:default_page folder)))]
          (when page
            (show-page request version versions page false)))))))


(defn download-zip [request version & [versions url is-url-version]]
  (if-let [zip (versions-data/get-zip (jdbc request) (:id version))]
    (-> zip
        clojure.java.io/input-stream
        response
        (header "Content-Length" (alength zip))
        (header "Content-Disposition" (str "attachment; filename=\"" (:key version) ".zip\""))
        (header "Content-Type" "application/zip, application/octet-stream")
        (header "Content-Description" "File Transfer")
        (header "Content-Transfer-Encoding" "binary"))
    (error-404 request)))


(defn generate-zip [request version & [versions url is-url-version]]
  (redisca/enqueue (redis request)
                   (-> request :component :config :zip-queue)
                   {:command "generate" :version version}))


(defn- search-page [request version & [versions url is-url-version]]
  (if-let [query (-> request :params :q)]
    (let [page-data {:id            -1
                     :version_id    (:id version)
                     :url           ""
                     :full_name     "Search page"
                     :content       (web-search/search-results-html request version query)
                     :last_modified (quot (System/currentTimeMillis) 1000)
                     :tags          [],
                     :config        {}}]
      (show-page request version versions page-data false))
    (error-404 request)))


(defn- search-data [request version & [versions url is-url-version]]
  (response (web-search/search-results request version)))


(defn- show-sitemap [request]
  (-> (response (sitemap/generate-sitemap (jdbc request)))
      (content-type "text/xml")))


(defn- show-sitemap-version [request]
  (let [version-name (-> request :params :version)]
    (-> (response (sitemap/generate-sitemap-version (jdbc request) version-name))
        (content-type "text/xml"))))


(defn- request-redirects [request]
  (let [redirects (-> request :component :redirects deref)]
    (if (empty? redirects)
      (response "Redirects empty")
      (response
        (->> redirects
             (sort-by first)
             (map #(str (first %) "\t >> \t" (second %)))
             (string/join "\n"))))))


(defn- show-page-data [request version versions url url-version & _]
  (if-let [page (pages-data/page-by-url (jdbc request) (:id version) url)]
    (response {:url            (:url page)
               :page           page
               :title          (:full_name page)
               :title-prefix   (title-prefix page (boolean url-version) (:key version))
               :versions       versions
               :is-url-version (boolean url-version)})
    (error-404 request)))


(defn- check-version-middleware [app]
  (fn [request]
    (let [version-key (-> request :route-params :version)
          page-url (-> request :route-params :*)
          versions (versions-data/get-page-versions (jdbc request) page-url)
          url-version (first (filter #(= version-key (:key %)) versions))
          version (if version-key url-version (first versions))]
      (if version
        (app request version versions page-url (boolean url-version))
        (error-404 request)))))


(defn- check-version-middleware-by-url [handler]
  (fn [request]
    (let [url (-> request :route-params :*)
          url-parts (filter seq (string/split url #"/"))
          possible-version-key (first url-parts)
          short-url (string/join "/" (drop 1 url-parts))]
      (if (versions-data/version-by-key (jdbc request) possible-version-key)
        (let [versions (versions-data/get-page-versions (jdbc request) short-url)
              url-version (first (filter #(= possible-version-key (:key %)) versions))]
          (when url-version
            (handler request url-version versions short-url url-version)))
        (let [versions (versions-data/get-page-versions (jdbc request) url)
              version (first versions)]
          (when version
            (handler request version versions url nil)))))))


(defn version-that-has-this-page-url [versions page-url]
  (first (filter #(.endsWith (:url %) page-url) versions)))


(defn- show-page-middleware [request version versions page-url url-version]
  (if (= "" page-url)
    (redirect (str "/" (:key version) "/Quick_Start"))
    (let [url (-> request :route-params :*)]
      (if (empty? page-url)
        (if-let [page (pages-data/page-by-url (jdbc request) (:id version) "")]
          (do
            (if (.endsWith url "/")
              (redirect (str "/" (utils/drop-last-slash url)) 301)
              (show-page request version versions page (boolean url-version))))
          (redirect (str "/" (:key version) "/Quick_Start")))

        (if-let [page (pages-data/page-by-url (jdbc request) (:id version) (utils/drop-last-slash page-url))]
          (if (.endsWith url "/")
            (redirect (str "/" (utils/drop-last-slash url)) 301)
            (show-page request version versions page (boolean url-version)))
          (if-let [folder (folders-data/get-folder-by-url (jdbc request) (:id version) page-url)]
            (redirect (str (when url-version (str "/" (:key version)))
                           "/" (:url folder)
                           "/" (:default_page folder)) 301)
            (error-404 request)))))))


(defn report [request]
  (let [version-key (-> request :route-params :version)
        report (vdata/version-report (jdbc request) version-key)]
    {:status  200
     :headers {"Access-Control-Allow-Origin" "*"}
     :body    report}))


(defn report-page [request]
  (let [version-key (-> request :route-params :version)
        report (vdata/version-report (jdbc request) version-key)]
    (analysis-page/page report version-key)))


(defn admin-panel [request]
  (let [versions (vdata/versions-full-info (jdbc request))]
    (admin-view/page {:title-prefix "Admin Panel | AnyChart Documentation\""
                      :description  "Admin Panel page"
                      :commit       (:commit (config request))} versions)))


(defn delete-version [request]
  (let [version-key (-> request :params :version)]
    (timbre/info "DELETE version request:" version-key)
    (vdata/remove-branch-by-key (jdbc request) version-key)
    (redirect "/_admin_")))


(defn rebuild-version [request]
  (let [params (-> request :params)]
    (timbre/info "REBUILD version request:" params)
    ;; just for not showing updated version in select on admin panel
    (when-let [version (:version params)]
      (vdata/remove-branch-by-key (jdbc request) version))
    (redisca/enqueue (redis request)
                     (-> request :component :config :queue)
                     (assoc params :cmd "generate"))))


(defroutes app-routes
           (route/resources "/")
           ;; management/admin routes
           (GET "/_admin_" [] admin-panel)
           (GET "/_redirects_" [] request-redirects)
           (GET "/_update_" [] request-update)
           (POST "/_update_" [] request-update)
           (POST "/_delete_" [] delete-version)
           (POST "/_rebuild_" [] rebuild-version)

           (GET "/" [] show-landing)
           (GET "/sitemap" [] show-sitemap)
           (GET "/sitemap.xml" [] show-sitemap)
           (GET "/sitemap/:version" [] show-sitemap-version)
           (GET "/sitemap.xml/:version" [] show-sitemap-version)
           (GET "/latest" [] show-latest)
           (GET "/latest/" [] show-latest)
           (GET "/latest/search" [] show-latest-search)
           (GET "/latest/*" [] try-show-latest-page)

           (GET "/:version/check/*" [] (check-version-middleware try-show-page))
           (GET "/:version/search" [] (check-version-middleware search-page))
           (POST "/:version/search-data" [] (check-version-middleware search-data))
           (GET "/:version/_generate-zip_" [] (check-version-middleware generate-zip))
           (GET "/:version/download" [] (check-version-middleware download-zip))
           (GET "/:version/report.json" [] report)
           (GET "/:version/report" [] report-page)

           (GET "/check/*" [] (check-version-middleware try-show-page))
           (GET "/search" [] (check-version-middleware search-page))
           (POST "/search-data" [] (check-version-middleware search-data))
           (GET "/_generate-zip_" [] (check-version-middleware generate-zip))
           (GET "/download" [] (check-version-middleware download-zip))

           (GET "/*-json" [] (check-version-middleware-by-url show-page-data))
           (GET "/*" [] (check-version-middleware-by-url show-page-middleware))
           (route/not-found show-404))


(defn wrap-google-analytics-optimize [handler]
  (fn [request]
    (let [user-agent (get-in request [:headers "user-agent"])
          is-ga-speed-insights (.contains (.toLowerCase user-agent)
                                          (.toLowerCase "Speed Insights"))]
      (handler (assoc request :is-ga-speed-insights is-ga-speed-insights)))))


(def app (-> (routes app-routes)
             wrap-keyword-params
             wrap-params
             wrap-redirect
             wrap-google-analytics-optimize))
