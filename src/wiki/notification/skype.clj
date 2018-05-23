(ns wiki.notification.skype
  (:require [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]
            [cheshire.core :as json]
            [clojure.string :as s]
            [wiki.util.utils :as utils]
            [wiki.config.core :as c]))


(defn- config [notifier] (-> notifier :config :skype))


(defn get-access-token [id key]
  (let [url "https://login.microsoftonline.com/common/oauth2/v2.0/token"
        data {"client_id"     id
              "scope"         "https://api.botframework.com/.default"
              "grant_type"    "client_credentials"
              "client_secret" key}
        resp @(http/post url {:form-params data})
        body (json/parse-string (:body resp) true)
        access-token (:access_token body)]
    access-token))


(defn send-msg [chat-id access-token message]
  (let [url (str "https://apis.skype.com/v2/conversations/" chat-id "/activities")
        data {:message {:content message}}
        headers {"Authorization" (str "Bearer " access-token)}
        resp @(http/post url {:body    (json/generate-string data)
                              :headers headers})]))


(defn send-message [{:keys [id key chat-id]} message]
  (try
    (let [access-token (get-access-token id key)]
      (send-msg chat-id access-token message))
    (catch Exception e
      (timbre/error "Skype send message error: " message))))


(defn send-release-message [conf version message]
  (when (and (:release-chat-id conf)
             (utils/released-version? version)
             (= (c/prefix) "prod"))
    (send-message (assoc conf :chat-id (:release-chat-id conf)) message)))


(defn font [text & [color size]]
  (str "<font "
       (when color (str "color=\"" color "\" "))
       (when size (str "size=\"" size "px\"")) ">"
       text "</font>"))
(defn b [text] (str "<b>" text "</b>"))
(defn u [text] (str "<u>" text "</u>"))
(defn i [text] (str "<i>" text "</i>"))


(defn start-building [notifier branches removed-branches queue-index]
  (let [msg (str "[Docs " (c/prefix) "] #" queue-index " - " (-> "start" (font "#4183C4") b) "\n"
                 (when (seq branches)
                   (str (b "Branches: ") (s/join ", " branches)))
                 (when (and (seq branches)
                            (seq removed-branches)) "\n")
                 (when (seq removed-branches)
                   (str (b "Removed branches: ") (s/join ", " removed-branches))))]
    (send-message (config notifier) msg)))


(defn complete-building [notifier branches removed-branches queue-index]
  (let [msg (str "[Docs " (c/prefix) "] #" queue-index " - " (-> "complete" (font "#36a64f") b) "\n"
                 (when (seq branches)
                   (str (b "Branches: ") (s/join ", " branches)))
                 (when (and (seq branches)
                            (seq removed-branches)) "\n")
                 (when (seq removed-branches)
                   (str (b "Removed branches: ") (s/join ", " removed-branches))))]
    (send-message (config notifier) msg)))


(defn complete-building-with-errors [notifier branches queue-index e]
  (let [msg (str "[Docs " (c/prefix) "] #" queue-index " - " (-> "error during processing!" (font "#d00000") b) "\n"
                 (when (seq branches)
                   (str (b "Branches: ") (s/join ", " branches)))
                 (when e
                   (-> (utils/format-exception e) (font "#777777" 11) i)))]
    (send-message (config notifier) msg)))


(defn start-version-building [notifier {author :author commit-message :message version :name commit :commit} queue-index]
  (let [msg (str "[Docs " (c/prefix) "] #" queue-index " " (b version)
                 " \"" commit-message "\" @" author " (" (subs commit 0 7) ") - "
                 (-> "start" (font "#4183C4")) "\n")]
    (send-message (config notifier) msg)
    (send-release-message (config notifier) version msg)))


(defn complete-version-building [notifier {author :author commit-message :message version :name commit :commit} queue-index]
  (let [msg (str "[Docs " (c/prefix) "] #" queue-index " " (b version)
                 " \"" commit-message "\" @" author " (" (subs commit 0 7) ") - "
                 (-> "complete" (font "#36a64f")) "\n")]
    (send-message (config notifier) msg)
    (send-release-message (config notifier) version msg)))


(defn complete-version-building-with-warnings [notifier {author :author commit-message :message version :name commit :commit} queue-index message]
  (let [msg (str "[Docs " (c/prefix) "] #" queue-index " " (b version)
                 " \"" commit-message "\" @" author " (" (subs commit 0 7) ") - "
                 (-> "complete with warnings" (font "#ff9800")) "\n"
                 message "\n<a href=\"" (c/domain) version "/report\">See full report</a>")]
    (send-message (config notifier) msg)
    (send-release-message (config notifier) version msg)))


(defn build-failed [notifier {author :author commit-message :message version :name commit :commit} queue-index & [e]]
  (let [msg (str "[Docs " (c/prefix) "] #" queue-index " " (b version)
                 " \"" commit-message "\" @" author " (" (subs commit 0 7) ") - "
                 (-> "failed" (font "#d00000") b) "\n"
                 (when e
                   (-> (utils/format-exception e) (font "#777777" 11) i)))]
    (send-message (config notifier) msg)
    (send-release-message (config notifier) version msg)))
