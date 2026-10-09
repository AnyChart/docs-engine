(ns wiki.notification.discord
  (:require [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]
            [cheshire.core :as json]
            [clojure.string :as s]
            [wiki.util.utils :as utils]
            [wiki.config.core :as c])
  (:import (org.apache.commons.lang3 StringEscapeUtils)))

(defn- config [notifier] (-> notifier :config :discord))

(def colors
  {:blue   4437377   ; #4183C4
   :green  3586383   ; #36a64f
   :red    13631488  ; #d00000
   :orange 16750848  ; #ff9800
   :gray   7829367}) ; #777777

(defn send-message [{:keys [webhook-url]} payload]
  (try
    @(http/post webhook-url
                {:body    (json/generate-string payload)
                 :headers {"Content-Type" "application/json"}})
    (catch Exception e
      (timbre/error "Discord send message error: " payload))))

(defn send-release-message [conf version payload]
  (when (and (utils/released-version? version)
             (= (c/prefix) "prod"))
    (send-message conf (assoc payload :is_release true))))

(defn start-building [notifier branches removed-branches queue-index]
  (let [fields (cond-> []
                 (seq branches)
                 (conj {:name "Branches"
                       :value (s/join ", " branches)})
                 (seq removed-branches)
                 (conj {:name "Removed branches"
                       :value (s/join ", " removed-branches)}))
        payload {:type "build"
                :embed {:title (str "[Docs " (c/prefix) "] #" queue-index " - Start")
                       :color (:blue colors)
                       :fields fields}}]
    (send-message (config notifier) payload)))

(defn complete-building [notifier branches removed-branches queue-index]
  (let [fields (cond-> []
                 (seq branches)
                 (conj {:name "Branches"
                       :value (s/join ", " branches)})
                 (seq removed-branches)
                 (conj {:name "Removed branches"
                       :value (s/join ", " removed-branches)}))
        payload {:type "build"
                :embed {:title (str "[Docs " (c/prefix) "] #" queue-index " - Complete")
                       :color (:green colors)
                       :fields fields}}]
    (send-message (config notifier) payload)))

(defn complete-building-with-errors [notifier branches queue-index e]
  (let [fields (cond-> []
                 (seq branches)
                 (conj {:name "Branches"
                       :value (s/join ", " branches)})
                 e
                 (conj {:name "Error"
                       :value (utils/format-exception e)}))
        payload {:type "build"
                :embed {:title (str "[Docs " (c/prefix) "] #" queue-index " - Error during processing!")
                       :color (:red colors)
                       :fields fields}}]
    (send-message (config notifier) payload)))

(defn start-version-building [notifier {author :author commit-message :message version :name commit :commit} queue-index]
  (let [payload {:type "version"
                 :embed {:title (str "[Docs " (c/prefix) "] #" queue-index)
                        :description (str "**Version:** " version "\n"
                                        "**Message:** " (StringEscapeUtils/escapeHtml4 commit-message) "\n"
                                        "**Author:** @" author "\n"
                                        "**Commit:** " (subs commit 0 7) "\n"
                                        "**Status:** Start")
                        :color (:blue colors)}}]
    (send-message (config notifier) payload)
    (send-release-message (config notifier) version payload)))

(defn complete-version-building [notifier {author :author commit-message :message version :name commit :commit}
                               queue-index report]
  (let [payload {:type "version"
                 :embed {:title (str "[Docs " (c/prefix) "] #" queue-index)
                        :description (str "**Version:** " version "\n"
                                        "**Message:** " (StringEscapeUtils/escapeHtml4 commit-message) "\n"
                                        "**Author:** @" author "\n"
                                        "**Commit:** " (subs commit 0 7) "\n"
                                        "**Status:** Complete"
                                        (when (:check-broken-links-disabled report) " (link-checker OFF)"))
                        :color (:green colors)}}]
    (send-message (config notifier) payload)
    (send-release-message (config notifier) version payload)))

(defn complete-version-building-with-warnings [notifier {author :author commit-message :message version :name commit :commit}
                                             queue-index report message]
  (let [payload {:type "version"
                 :embed {:title (str "[Docs " (c/prefix) "] #" queue-index)
                        :description (str "**Version:** " version "\n"
                                        "**Message:** " (StringEscapeUtils/escapeHtml4 commit-message) "\n"
                                        "**Author:** @" author "\n"
                                        "**Commit:** " (subs commit 0 7) "\n"
                                        "**Status:** Complete with warnings"
                                        (when (:check-broken-links-disabled report) " (link-checker OFF)"))
                        :fields [{:name "Warnings"
                                :value message}
                               {:name "Report"
                                :value (str (c/domain) version "/report")}]
                        :color (:orange colors)}}]
    (send-message (config notifier) payload)
    (send-release-message (config notifier) version payload)))

(defn build-failed [notifier {author :author commit-message :message version :name commit :commit} queue-index & [e]]
  (let [payload {:type "version"
                 :embed {:title (str "[Docs " (c/prefix) "] #" queue-index)
                        :description (str "**Version:** " version "\n"
                                        "**Message:** " (StringEscapeUtils/escapeHtml4 commit-message) "\n"
                                        "**Author:** @" author "\n"
                                        "**Commit:** " (subs commit 0 7) "\n"
                                        "**Status:** Failed")
                        :fields (when e
                                 [{:name "Error"
                                   :value (utils/format-exception e)}])
                        :color (:red colors)}}]
    (send-message (config notifier) payload)
    (send-release-message (config notifier) version payload)))

(defn sample-parsing-error [notifier version page-url]
  (let [webhook-url (-> notifier :config :discord :webhook-url)
        attachment {:embeds [{:color     16776960  ; "warning" color in decimal (yellow)
                            :description (str (c/domain) version "/" page-url " sample parsing error!")
                            :footer     {:text "Sample Parsing Error"}}]}]
    (http/post webhook-url
               {:body (json/generate-string attachment)
                :headers {"Content-Type" "application/json"}})))

(defn image-format-error [notifier version page-url]
  (send-message notifier
                {:color       (:warning colors)
                 :description (str (c/domain) version "/" page-url " image format error!")
                 :footer      {:text "Image Format Error"}}))

(defn sample-not-available [notifier version page-url]
  (send-message notifier
                {:color       (:warning colors)
                 :description (str (c/domain) version "/" page-url " sample not available!")
                 :footer      {:text "Sample Not Available"}}))

(defn notify-404 [notifier path]
  (let [error-webhook-url (-> notifier :config :discord :error-webhook-url)] ; Separate webhook for errors
    (http/post error-webhook-url
               {:body    (json/generate-string
                          {:embeds [{:color       (:danger colors)
                                   :description  (str (c/domain) " 404: " path)
                                   :footer      {:text "404 Error"}
                                   }]})
                :headers {"Content-Type" "application/json"}})))
