(ns wiki.components.notifier
  (:require [com.stuartsierra.component :as component]
            [cheshire.core :refer [generate-string]]
            [org.httpkit.client :as http]
            [wiki.notification.slack :as slack]
            [wiki.notification.skype :as skype]))

(defrecord Notifier [config]
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn new-notifier [config]
  (map->Notifier {:config config :messages (agent [])}))

(defn start-building [notifier branches removed-branches queue-index]
  (slack/start-building notifier branches removed-branches queue-index)
  (skype/start-building notifier branches removed-branches queue-index))

(defn complete-building [notifier branches removed-branches queue-index]
  (slack/complete-building notifier branches removed-branches queue-index)
  (skype/complete-building notifier branches removed-branches queue-index))

(defn complete-building-with-errors [notifier branches queue-index & [e]]
  (slack/complete-building-with-errors notifier branches queue-index e)
  (skype/complete-building-with-errors notifier branches queue-index e))

(defn start-version-building [notifier version queue-index]
  (slack/start-version-building notifier version queue-index)
  (skype/start-version-building notifier version queue-index))

(defn complete-version-building [notifier version queue-index report]
  (let [direct-links (count (set (mapcat :direct-links report)))
        canonical-links (count (set (mapcat :non-canonical-links report)))
        http-links (count (set (mapcat :http-links report)))
        env-links (count (set (mapcat :env-links report)))
        sample-not-available (count (set (mapcat :sample-not-available report)))
        sample-parsing-error (count (set (mapcat :sample-parsing-error report)))
        image-format-error (count (set (mapcat :image-format-error report)))
        msg-coll [(when (pos? direct-links) (str "direct links: " direct-links))
                  (when (pos? canonical-links) (str "non canonical links: " canonical-links))
                  (when (pos? http-links) (str "http links: " http-links))
                  (when (pos? env-links) (str "env links: " env-links))
                  (when (pos? sample-not-available) (str "unavailable samples: " sample-not-available))
                  (when (pos? sample-parsing-error) (str "parsing samples errors: " sample-parsing-error))
                  (when (pos? image-format-error) (str "parsing images errors: " image-format-error))]
        msg (clojure.string/join ", " (filter some? msg-coll))
        res-msg (if (= 0 direct-links canonical-links env-links http-links
                       sample-not-available sample-parsing-error image-format-error)
                  "good job, everything is ok!"
                  msg)]
    ;(prn report)
    ;(prn "==complete-version-building " direct-links canonical-links http-links env-links res-msg)
    (slack/complete-version-building notifier version queue-index)
    (skype/complete-version-building notifier version queue-index res-msg)))

(defn build-failed [notifier version queue-index & [e]]
  (slack/build-failed notifier version queue-index e)
  (skype/build-failed notifier version queue-index e))

(defn sample-parsing-error [notifier version page-url]
  (slack/sample-parsing-error notifier version page-url))

(defn image-format-error [notifier version page-url]
  (slack/image-format-error notifier version page-url))

(defn sample-not-available [notifier version page-url]
  (slack/sample-not-available notifier version page-url))

(defn notify-404 [notifier path]
  (slack/notify-404 notifier path))
