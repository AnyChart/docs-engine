(ns wiki.notification.slack
  (:require [cheshire.core :refer [generate-string]]
            [org.httpkit.client :as http]
            [wiki.util.utils :as utils]
            [wiki.config.core :as c]
            [clojure.string :as string]))


(defn- channel [notifier] (-> notifier :config :slack :channel))
(defn- username [notifier] (-> notifier :config :slack :username))
(defn- token [notifier] (-> notifier :config :slack :token))


(defn- notify-attach-delay [notifier attachment]
  (send (:messages notifier) conj attachment))


(defn- notify-attach [notifier attachments]
  (send (:messages notifier)
        (fn [messages attachments]
          (http/post (str "https://anychart-team.slack.com/services/hooks/incoming-webhook?token=" (token notifier))
                     {:form-params
                      {:payload (generate-string
                                  {:attachments (concat messages attachments)
                                   :mrkdwn      true
                                   :channel     (channel notifier)
                                   :username    (username notifier)})}})
          [])
        attachments))


(defn start-building [notifier branches removed-branches queue-index]
  (let [attachments [{:color     "#4183C4"
                      :text      (str "#" queue-index " docs `" (c/prefix) "` - start")
                      :mrkdwn_in ["text", "pretext"]
                      :fields    (if (seq branches)
                                   [{:title "Branches"
                                     :value (string/join ", " branches)
                                     :short true}]
                                   [])}]
        removed-fields (when (seq removed-branches)
                         [{:title "Removed branches"
                           :value (string/join ", " removed-branches)
                           :short true}])]
    (notify-attach notifier (update-in attachments [0 :fields] concat removed-fields))))


(defn complete-building [notifier branches removed-branches queue-index]
  (let [attachments [{:color     "#36a64f"
                      :text      (str "#" queue-index " docs `" (c/prefix) "` - complete")
                      :mrkdwn_in ["text", "pretext"]
                      :fields    (if (seq branches)
                                   [{:title "Branches"
                                     :value (string/join ", " branches)
                                     :short true}]
                                   [])}]
        removed-fields (when (seq removed-branches)
                         [{:title "Removed branches"
                           :value (string/join ", " removed-branches)
                           :short true}])]
    (notify-attach notifier (update-in attachments [0 :fields] concat removed-fields))))


(defn complete-building-with-errors [notifier branches queue-index & [e]]
  (let [attachments [{:color     "danger"
                      :text      (str "#" queue-index " docs `" (c/prefix) "` - complete with errors"
                                      (when e (str "\n```" (utils/format-exception e) "```")))
                      :mrkdwn_in ["text", "pretext"]
                      :fields    (if (seq branches)
                                   [{:title "Branches"
                                     :value (string/join ", " branches)
                                     :short true}]
                                   [])}]]
    (notify-attach notifier attachments)))


(defn start-version-building [notifier version queue-index]
  (let [attachments [{:color     "#4183C4"
                      :text      (str "#" queue-index " docs `" (c/prefix) "` - *" version "* start")
                      :mrkdwn_in ["text"]}]]
    (notify-attach notifier attachments)))


(defn complete-version-building [notifier version queue-index]
  (let [attachments [{:color     "#36a64f"
                      :text      (str "#" queue-index " docs `" (c/prefix) "` - *" version "* complete")
                      :mrkdwn_in ["text"]}]]
    (notify-attach notifier attachments)))


(defn build-failed [notifier version queue-index & [e]]
  (let [attachments [{:color     "danger"
                      :text      (str "#" queue-index " docs `" (c/prefix) "` - *" version "* failed"
                                      (when e (str "\n```" (utils/format-exception e) "```")))
                      :mrkdwn_in ["text"]}]]
    (notify-attach notifier attachments)))


(defn sample-parsing-error [notifier version page-url]
  (let [attachment {:color     "warning"
                    :text      (str (c/domain) version "/" page-url " sample parsing error!")
                    :mrkdwn_in ["text"]}]
    (notify-attach-delay notifier attachment)))


(defn image-format-error [notifier version page-url]
  (let [attachment {:color     "warning"
                    :text      (str (c/domain) version "/" page-url " image format error!")
                    :mrkdwn_in ["text"]}]
    (notify-attach-delay notifier attachment)))


(defn sample-not-available [notifier version page-url]
  (let [attachment {:color     "warning"
                    :text      (str (c/domain) version "/" page-url " sample not available!")
                    :mrkdwn_in ["text"]}]
    (notify-attach-delay notifier attachment)))


(defn notify-404 [notifier path]
  (http/post (str "https://anychart-team.slack.com/services/hooks/incoming-webhook?token=" (token notifier))
             {:form-params
              {:payload (generate-string
                          {:text     (str (c/domain) " 404: " path)
                           :channel  "#docs-404-errors"
                           :username (username notifier)})}}))
