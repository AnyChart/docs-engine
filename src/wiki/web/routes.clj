(ns wiki.web.routes
  (:require [selmer.parser :refer [render-file add-tag!]]
            [compojure.core :refer [defroutes routes GET POST]]
            [compojure.route :as route]
            [ring.util.response :refer [redirect response content-type file-response header]]
            [ring.util.request :refer [request-url]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response]]
            [wiki.components.redis :as redisca]
            [wiki.components.notifier :refer [notify-404]]
            [wiki.data.versions :as versions-data]
            [wiki.data.pages :as pages-data]
            [wiki.data.folders :as folders-data]
            [wiki.data.sitemap :as sitemap]
            [wiki.data.search :as search]
            [wiki.util.utils :as utils]
            [wiki.web.tree :refer [tree-view tree-view-local]]
            [wiki.web.redirects :refer [wrap-redirect]]
            [criterium.core :refer [bench]])
  (:import (com.googlecode.htmlcompressor.compressor HtmlCompressor ClosureJavaScriptCompressor)
           (com.google.javascript.jscomp CompilationLevel)))

(add-tag! :tree-view (fn [args context-map]
                       (let [entries (get context-map (keyword (first args)))]
                         (reduce str (map #(tree-view % (:version context-map) (:is-url-version context-map)) entries)))))

(add-tag! :tree-view-local (fn [args context-map]
                             (let [entries (get context-map (keyword (first args)))
                                   path (get context-map (keyword (second args)))]
                               (reduce str (map #(tree-view-local % (:version context-map) path) entries)))))

(defn- jdbc [request]
  (-> request :component :jdbc))

(defn- sphinx [request]
  (-> request :component :sphinx))

(defn- redis [request]
  (-> request :component :redis))

(defn- notifier [request]
  (-> request :component :notifier))

(defn offline-generator [request]
  (-> request :component :offline-generator))

(defn- title-prefix [page]
  (or (-> page :config :title)
      (-> page :url utils/url->title)))

(defn- show-404 [request]
  (render-file "templates/404.selmer" {}))

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
                   "generate"))

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
  (let [page (render-file "templates/page.selmer" {:version              (:key version)
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
                                                   :title-prefix         (title-prefix page)
                                                   :description          (utils/page-description (:content page))
                                                   :page                 page
                                                   :versions             versions
                                                   :is-ga-speed-insights (:is-ga-speed-insights request)})
        html-compressor (HtmlCompressor.)]
    (.setRemoveIntertagSpaces html-compressor true)
    (.setRemoveQuotes html-compressor true)
    ;(.setJavaScriptCompressor html-compressor (ClosureJavaScriptCompressor. CompilationLevel/SIMPLE_OPTIMIZATIONS))
    ;(.setCompressJavaScript html-compressor true)
    (.compress html-compressor page)))

(defn- show-landing [request]
  (let [url "Quick_Start/Quick_Start"
        versions (versions-data/get-page-versions (jdbc request) url)
        version (first versions)
        page (pages-data/page-by-url (jdbc request) (:id version) url)]
    (if page
      (show-page request version versions page false)
      (let [folder (folders-data/get-folder-by-url (jdbc request) (:id version) "Quick_Start")
            page (pages-data/page-by-url (jdbc request) (:id version)
                                         (str "Quick_Start/" (:default_page folder)))]
        (show-page request version versions page false)))))

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

(defn- format-search-result [result query version]
  (let [words (clojure.string/split query #" ")
        url (:url result)
        title (reduce (fn [res q]
                        (clojure.string/replace res
                                                (re-pattern (str "(?i)" (clojure.string/re-quote-replacement q)))
                                                #(str "{b}" % "{eb}")))
                      (:url result) words)
        title (-> title
                  (clojure.string/replace #"_" " ")
                  (clojure.string/split #"/"))
        title (map #(-> %
                        (clojure.string/replace #"\{b\}" "<span class='match'>")
                        (clojure.string/replace #"\{eb\}" "</span>"))
                   title)]
    (assoc result
      :title (str (clojure.string/join " / " (drop-last 1 title))
                  " / <a href='/" version "/" url "'>" (first (take-last 1 title)) "</a>"))))

(defn- search-results [request version]
  (map #(format-search-result % (-> request :params :q) (:key version))
       (search/search-for (sphinx request)
                          (-> request :params :q)
                          (:id version)
                          (:key version)
                          (-> request :component :sphinx :config :table))))

(defn- search-page [request version & [versions url is-url-version]]
  (if-let [query (-> request :params :q)]
    (render-file "templates/search.selmer" {:version          (:key version)
                                            :anychart-url     (utils/anychart-bundle-path (:key version))
                                            :anychart-css-url (utils/anychart-bundle-css-url (:key version))
                                            :is-url-version   is-url-version
                                            :tree             (versions-data/tree-data (jdbc request) (:id version))
                                            :query            query
                                            :versions         (versions-data/versions (jdbc request))
                                            :results          (search-results request version)})
    (error-404 request)))

(defn- search-data [request version & [versions url is-url-version]]
  (response (search-results request version)))

(defn- show-sitemap [request]
  (-> (response (sitemap/generate-sitemap (jdbc request)))
      (content-type "text/xml")))

(defn- request-redirects [request]
  (let [redirects (-> request :component :redirects deref)]
    (if (empty? redirects)
      (response "Redirects empty")
      (response
        (->> redirects
             (map #(str (first %) "\t >> \t" (second %)))
             (clojure.string/join "\n"))))))

(defn- show-page-data [request version versions url & _]
  (if-let [page (pages-data/page-by-url (jdbc request) (:id version) url)]
    (response {:url          (:url page)
               :page         page
               :title        (:full_name page)
               :title-prefix (title-prefix page)
               :versions     versions})
    (error-404 request)))

(defn- check-version-middleware [app]
  (fn [request]
    (let [version-key (-> request :route-params :version)
          page-url (-> request :route-params :*)
          versions (versions-data/get-page-versions (jdbc request) page-url)
          url-version (first (filter #(= version-key (:key %)) versions))
          version (or url-version (first versions))]
      ;(prn "Check version middleware2: " version page-url)
      (if version
        (app request version versions page-url (boolean url-version))
        (error-404 request)))))

(defn- check-version-middleware-by-url [handler]
  (fn [request]
    (let [url (-> request :route-params :*)
          url-parts (filter seq (clojure.string/split url #"/"))
          short-url (clojure.string/join "/" (drop 1 url-parts))
          versions (versions-data/get-page-versions (jdbc request) url short-url)
          url-version (first (filter #(= (first url-parts) (:key %)) versions))
          version (or url-version (first versions))
          page-url (if url-version short-url url)]
      (handler request version versions page-url url-version))))

(defn- show-page-middleware [request version versions page-url url-version]
  ;(prn :show-page-middleware (-> request :route-params :*) version page-url url-version)
  (if (empty? page-url)
    (redirect (str "/" (:key version) "/Quick_Start"))
    (if-let [page (pages-data/page-by-url (jdbc request) (:id version) page-url)]
      (show-page request version versions page (boolean url-version))
      (if-let [folder (folders-data/get-folder-by-url (jdbc request) (:id version) page-url)]
        (redirect (str (when url-version (str "/" (:key version)))
                       "/" (:url folder)
                       "/" (:default_page folder)))
        (error-404 request)))))

(defroutes app-routes
           (route/resources "/")
           (GET "/_update_" [] request-update)
           (GET "/_redirects_" [] request-redirects)
           (POST "/_update_" [] request-update)
           (GET "/" [] show-landing)
           (GET "/sitemap" [] show-sitemap)
           (GET "/sitemap.xml" [] show-sitemap)
           (GET "/latest" [] show-latest)
           (GET "/latest/" [] show-latest)
           (GET "/latest/search" [] show-latest-search)
           (GET "/latest/*" [] try-show-latest-page)

           (GET "/:version/check/*" [] (check-version-middleware try-show-page))
           (GET "/:version/search" [] (check-version-middleware search-page))
           (POST "/:version/search-data" [] (check-version-middleware search-data))
           (GET "/:version/_generate-zip_" [] (check-version-middleware generate-zip))
           (GET "/:version/download" [] (check-version-middleware download-zip))

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
