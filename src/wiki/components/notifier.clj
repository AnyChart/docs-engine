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

(defn complete-version-building [notifier version queue-index]
  (slack/complete-version-building notifier version queue-index)
  (skype/complete-version-building notifier version queue-index))

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
