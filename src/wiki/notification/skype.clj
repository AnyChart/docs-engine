(ns wiki.notification.skype
  (:require [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]
            [cheshire.core :as json]
            [clojure.string :as s]
            [wiki.util.utils :as utils]))

(def config {:id      "5bab828d-d6b2-4c0f-a955-82b088e30bcb"
             :key     "L2q5SBiXYqPDHVmq36vjgwe"
             :chat-id "19:58cbaa008fc640bab8c3cf71e0e2d51a@thread.skype"})

(defn get-access-token [id key]
  (let [url "https://login.microsoftonline.com/common/oauth2/v2.0/token"
        data {"client_id"     id
              "scope"         "https://graph.microsoft.com/.default"
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

(defn- config [notifier] (-> notifier :config :skype))
(defn- prefix [notifier] (-> notifier :config :skype :prefix))

(defn font [text & [color size]]
  (str "<font "
       (when color (str "color=\"" color "\" "))
       (when size (str "size=\"" size "px\"")) ">"
       text "</font>"))

(defn b [text] (str "<b>" text "</b>"))
(defn u [text] (str "<u>" text "</u>"))
(defn i [text] (str "<i>" text "</i>"))

(defn start-building [notifier branches removed-branches queue-index]
  (let [msg (str "#" queue-index " docs " (-> notifier prefix (font "#cc0066" 11) u) " - " (-> "start" (font "#4183C4") b) "\n"
                 (when (seq branches)
                   (str (b "Branches: ") (s/join ", " branches)))
                 (when (and (seq branches)
                            (seq removed-branches)) "\n")
                 (when (seq removed-branches)
                   (str (b "Removed branches: ") (s/join ", " removed-branches))))]
    (send-message (config notifier) msg)))

(defn complete-building [notifier branches removed-branches queue-index]
  (let [msg (str "#" queue-index " docs " (-> notifier prefix (font "#cc0066" 11) u) " - " (-> "complete" (font "#36a64f") b) "\n"
                 (when (seq branches)
                   (str (b "Branches: ") (s/join ", " branches)))
                 (when (and (seq branches)
                            (seq removed-branches)) "\n")
                 (when (seq removed-branches)
                   (str (b "Removed branches: ") (s/join ", " removed-branches))))]
    (send-message (config notifier) msg)))

(defn complete-building-with-errors [notifier branches queue-index e]
  (let [msg (str "#" queue-index " docs " (-> notifier prefix (font "#cc0066" 11) u) " - " (-> "complete with errors!" (font "#d00000") b) "\n"
                 (when (seq branches)
                   (str (b "Branches: ") (s/join ", " branches))))]
    (send-message (config notifier) msg)))


(defn start-version-building [notifier {author :author commit-message :message version :name} queue-index]
  (let [msg (str "#" queue-index " docs " (-> notifier prefix (font "#cc0066" 11) u) " - "
                 (b version)
                 " " commit-message " - " author
                 (-> " start" (font "#4183C4") b) "\n")]
    (send-message (config notifier) msg)))

(defn complete-version-building [notifier version queue-index message]
  (let [msg (str "#" queue-index " docs " (-> notifier prefix (font "#cc0066" 11) u) " - " (b version) (-> " complete" (font "#36a64f") b) " " message  "\n")]
    (send-message (config notifier) msg)))

(defn base-url-by-prefix [prefix]
  (case (keyword prefix)
    :prod "https://docs.anychart.com/"
    :stg "http://docs.anychart.stg/"
    :local "http://localhost:8080/"))

(defn complete-version-building-with-warnings [notifier version queue-index message]
  (let [msg (str "#" queue-index " docs " (-> notifier prefix (font "#cc0066" 11) u) " - " (b version) (-> " complete with warnings" (font "#daa038") b)
                 "\n" message  "\nSee full report at: " (base-url-by-prefix (-> notifier prefix)) version "/report"  )]
    (send-message (config notifier) msg)))

(defn build-failed [notifier version queue-index & [e]]
  (let [msg (str "#" queue-index " docs " (-> notifier prefix (font "#cc0066" 11) u) " - " (b version) (-> " failed" (font "#d00000") b) "\n"
                 (when e
                   (-> (utils/format-exception e) (font "#777777" 11) i)))]
    (send-message (config notifier) msg)))
