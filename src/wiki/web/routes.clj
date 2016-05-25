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
            [wiki.web.tree :refer [tree-view tree-view-local]]))

(add-tag! :tree-view (fn [args context-map]
                       (let [entries (get context-map (keyword (first args)))]
                         (reduce str (map #(tree-view % (:version context-map)) entries)))))

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
    (if (pages-data/page-exists? (jdbc request) (:id version) url)
      (redirect (str "/" (:key version) "/" url))
      (redirect (str "/" (:key version) "/Quick_Start")))))

(defn- show-page-data [request version page]
  (response {:url   (:url page)
             :page  page
             :title (:full_name page)}))

(defn- show-page [request version page]
  (let [versions (versions-data/versions (jdbc request))]
    (render-file "templates/page.selmer" {:version        (:key version)
                                          :actual-version (first versions)
                                          :old            (not= (first versions) (:key version))
                                          :tree           (versions-data/tree-data (jdbc request)
                                                                                   (:id version))
                                          :url            (:url page)
                                          :title          (:full_name page)
                                          :page           page
                                          :versions       versions})))

(defn download-zip [request version]
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

(defn generate-zip [request version]
  (redisca/enqueue (redis request)
                   (-> request :component :config :zip-queue)
                   {:command "generate" :version version}))

(defn- try-show-latest-page [request]
  (let [version (versions-data/default (jdbc request))]
    (redirect (str "/" version "/" (-> request :route-params :*)))))

(defn- format-search-result [result query version]
  (let [words (clojure.string/split query #" ")
        url (:url result)
        title (reduce (fn [res q]
                        (clojure.string/replace res
                                                (re-pattern (str "(?i)"
                                                                 (clojure.string/re-quote-replacement
                                                                   q)))
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
  (render-file "templates/search.selmer" {:version  (:key version)
                                          :tree     (versions-data/tree-data (jdbc request)
                                                                             (:id version))
                                          :query    (-> request :params :q)
                                          :versions (versions-data/versions (jdbc request))
                                          :results  (map #(format-search-result
                                                           %
                                                           (-> request :params :q)
                                                           (:key version))
                                                         (search/search-for (sphinx request)
                                                                            (-> request
                                                                                :params :q)
                                                                            (:id version)
                                                                            (:key version)
                                                                            (-> request
                                                                                :component
                                                                                :sphinx
                                                                                :config
                                                                                :table)))}))

(defn- search-data [request version]
  (response (map #(format-search-result
                   % (-> request :params :q) (:key version))
                 (search/search-for (sphinx request)
                                    (-> request
                                        :params :q)
                                    (:id version)
                                    (:key version)
                                    (-> request
                                        :component
                                        :sphinx
                                        :config
                                        :table)))))

(defn- check-version-middleware [app]
  (fn [request]
    (if-let [version (versions-data/version-by-key (jdbc request)
                                                   (-> request :route-params :version))]
      (app request version)
      (error-404 request))))

(defn- show-sitemap [request]
  (-> (response (sitemap/generate-sitemap (jdbc request)))
      (content-type "text/xml")))

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
           (GET "/sitemap" [] show-sitemap)
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
           (GET "/:version/_generate-zip_" [] (check-version-middleware generate-zip))
           (GET "/:version/download" [] (check-version-middleware download-zip))
           (GET "/:version/*" [] (check-folder-middleware show-page))
           (route/not-found show-404))

(def app (-> (routes app-routes)
             wrap-keyword-params
             wrap-params))
