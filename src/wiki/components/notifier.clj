(ns wiki.components.notifier
  (:require [com.stuartsierra.component :as component]
            [cheshire.core :refer [generate-string]]
            [org.httpkit.client :as http]))

(defrecord Notifier [config]
  component/Lifecycle

  (start [this] this)
  (stop [this] this))

(defn new-notifier [config]
  (map->Notifier {:config config :messages (agent [])}))

(defn notify-raw [notifier text]
  (http/post (str "https://anychart-team.slack.com/services/hooks/incoming-webhook?token="
                  (-> notifier :config :token))
             {:form-params
              {:payload (generate-string
                          {:text text
                           :mrkdwn true
                           :channel (-> notifier :config :channel)
                           :username (-> notifier :config :username)})}}))

(defn- domain [notifier] (-> notifier :config :domain))
(defn- prefix [notifier] (-> notifier :config :prefix))

(defn- add-domain [notifier text]
  (str (-> notifier :config :domain) " " text))

(defn- notify-simple [notifier text]
  (notify-raw notifier text))

(defn notify-delay [notifier message]
  (send (:messages notifier) conj message))

(defn notify [notifier message]
  (send (:messages notifier) (fn [messages message]
                              (let [all-msg (conj messages message)
                                    total-msg (clojure.string/join "\n" all-msg)]
                                (notify-raw notifier total-msg))
                              []) message))

(defn- notify-attach-delay [notifier attachment]
  (send (:messages notifier) conj attachment))

(defn- notify-attach-raw [notifier attachments]
  (http/post (str "https://anychart-team.slack.com/services/hooks/incoming-webhook?token="
                  (-> notifier :config :token))
             {:form-params
              {:payload (generate-string
                          {:attachments attachments
                           :mrkdwn true
                           :channel (-> notifier :config :channel)
                           :username (-> notifier :config :username)})}}))

(defn- notify-attach [notifier attachments]
  (send (:messages notifier)
        (fn [messages attachments]
          (http/post (str "https://anychart-team.slack.com/services/hooks/incoming-webhook?token="
                         (-> notifier :config :token))
                     {:form-params
                      {:payload (generate-string
                                  {:attachments (concat messages attachments)
                                   :mrkdwn      true
                                   :channel     (-> notifier :config :channel)
                                   :username    (-> notifier :config :username)})}})
          [])
        attachments))

(defn start-building [notifier branches removed-branches queue-index]
  (let [attachments [{:color  "#4183C4"
                      :text (str "#" queue-index " docs `" (prefix notifier) "` - start")
                      :mrkdwn_in ["text", "pretext"]
                      :fields (if (seq branches)
                                [{:title "Branches"
                                 :value (clojure.string/join ", " branches)
                                 :short true}]
                                [])}]
        removed-fields (when (seq removed-branches)
                         [{:title "Removed branches"
                           :value (clojure.string/join ", " removed-branches)
                           :short true}])]
    (notify-attach notifier (update-in attachments [0 :fields] concat removed-fields))))

(defn complete-building [notifier branches removed-branches queue-index]
   (let [attachments [{:color  "#36a64f"
                      :text (str "#" queue-index " docs `" (prefix notifier) "` - complete")
                      :mrkdwn_in ["text", "pretext"]
                      :fields (if (seq branches)
                                [{:title "Branches"
                                  :value (clojure.string/join ", " branches)
                                  :short true}]
                                [])}]
        removed-fields (when (seq removed-branches)
                         [{:title "Removed branches"
                           :value (clojure.string/join ", " removed-branches)
                           :short true}])]
    (notify-attach notifier (update-in attachments [0 :fields] concat removed-fields))))

(defn complete-building-with-errors [notifier branches queue-index]
  (let [attachments [{:color  "danger"
                      :text (str "#" queue-index " docs `" (prefix notifier) "` - complete with errors")
                      :mrkdwn_in ["text", "pretext"]
                      :fields (if (seq branches)
                                [{:title "Branches"
                                  :value (clojure.string/join ", " branches)
                                  :short true}]
                                [])}]]
    (notify-attach notifier attachments)))

(defn start-version-building [notifier version queue-index]
  (let [attachments [{:color  "#4183C4"
                      :text (str "#" queue-index " docs `" (prefix notifier) "` - *" version "* start" )
                      :mrkdwn_in ["text"]}]]
    (notify-attach notifier attachments)))

(defn complete-version-building [notifier version queue-index]
  (let [attachments [{:color  "#36a64f"
                      :text (str "#" queue-index " docs `" (prefix notifier) "` - *" version "* complete")
                      :mrkdwn_in ["text"]}]]
    (notify-attach notifier attachments)))

(defn build-failed [notifier version queue-index]
  (let [attachments [{:color  "danger"
                      :text (str "#" queue-index " docs `" (prefix notifier) "` - *" version "* failed")
                      :mrkdwn_in ["text"]}]]
    (notify-attach notifier attachments)))

(defn sample-parsing-error [notifier version page-url]
  (let [attachment {:color     "warning"
                    :text (str (domain notifier) version "/" page-url " sample parsing error!")
                    :mrkdwn_in ["text"]}]
    (notify-attach-delay notifier attachment)))

(defn image-format-error [notifier version page-url]
  (let [attachment {:color     "warning"
                    :text (str (domain notifier) version "/" page-url " image format error!")
                    :mrkdwn_in ["text"]}]
    (notify-attach-delay notifier attachment)))

(defn sample-not-available [notifier version page-url]
  (let [attachment {:color     "warning"
                    :text  (str (domain notifier) version "/" page-url " sample not available!")
                    :mrkdwn_in ["text"]}]
    (notify-attach-delay notifier attachment)))

(defn notify-404 [notifier path]
  (http/post (str "https://anychart-team.slack.com/services/hooks/incoming-webhook?token="
                  (-> notifier :config :token))
             {:form-params
              {:payload (generate-string
                         {:text (str (-> notifier :config :domain) " 404: " path)
                          :channel "#docs-404-errors"
                          :username (-> notifier :config :username)})}}))
