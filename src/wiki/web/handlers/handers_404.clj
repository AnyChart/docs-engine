(ns wiki.web.handlers.handers-404
  (:require [wiki.components.notifier :as notifications]
            [wiki.web.helpers :refer :all]
            [wiki.views.page404.page404 :as page-404]
            [compojure.route :as route]
            [ring.util.request :refer [request-url]]))


(defn show-404 [request]
  (page-404/page {:title-prefix "Not found | AnyChart Documentation"
                  :description  "404 Not found page"
                  :commit       (:commit (config request))}))


(defn error-404 [request]
  (let [referrer (get-in request [:headers "referer"])
        ua (get-in request [:headers "user-agent"])]
    (when (not (.contains ua "Slackbot"))
      (if referrer
        (notifications/notify-404 (notifier request) (str (request-url request) " from " referrer))
        (notifications/notify-404 (notifier request) (request-url request)))))
  (route/not-found (show-404 request)))
