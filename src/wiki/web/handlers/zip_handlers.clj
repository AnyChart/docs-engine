(ns wiki.web.handlers.zip-handlers
  (:require [wiki.data.versions :as versions-data]
            [wiki.components.redis :as redisca]
            [wiki.web.helpers :refer :all]
            [ring.util.response :refer [redirect response content-type file-response header]]
            [clojure.java.io :as io]))


(defn download-zip [request version & [versions url is-url-version]]
  (when-let [zip (versions-data/get-zip (jdbc request) (:id version))]
    (-> zip
        io/input-stream
        response
        (header "Content-Length" (alength zip))
        (header "Content-Disposition" (str "attachment; filename=\"" (:key version) ".zip\""))
        (header "Content-Type" "application/zip, application/octet-stream")
        (header "Content-Description" "File Transfer")
        (header "Content-Transfer-Encoding" "binary"))))


(defn generate-zip [request version & [versions url is-url-version]]
  (redisca/enqueue (redis request)
                   (-> request :component :config :zip-queue)
                   {:command "generate" :version version}))