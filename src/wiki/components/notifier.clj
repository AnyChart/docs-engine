(ns wiki.components.notifier
  (:require [com.stuartsierra.component :as component]
            [cheshire.core :refer [generate-string]]
            [org.httpkit.client :as http]))

(defrecord Notifier [config]
  component/Lifecycle

  (start [this] this)
  (stop [this] this))

(defn new-notifier [config]
  (map->Notifier {:config config}))

(defn- notify [notifier text]
  (http/post (str "https://anychart-team.slack.com/services/hooks/incoming-webhook?token="
                  (-> notifier :config :token))
             {:form-params
              {:payload (generate-string
                         {:text (str (-> notifier :config :domain) " " text)
                          :channel (-> notifier :config :channel)
                          :username (-> notifier :config :username)})}}))

(defn delete-branches [notifier branches]
  (notify notifier (str "branches deleted: " (clojure.string/join ", " branches))))

(defn start-building [notifier]
  (notify notifier "start docs update"))

(defn complete-building [notifier]
  (notify notifier "docs update completed"))

(defn versions-for-build [notifier versions]
  (notify notifier (str "build list: " (clojure.string/join ", " versions))))

(defn start-version-building [notifier version]
  (notify notifier (str "start building " version)))

(defn complete-version-building [notifier version]
  (notify notifier (str version " build completed")))

(defn build-failed [notifier version]
  (notify notifier (str version " build FAILED")))

(defn notify-404 [notifier path]
  (http/post (str "https://anychart-team.slack.com/services/hooks/incoming-webhook?token="
                  (-> notifier :config :token))
             {:form-params
              {:payload (generate-string
                         {:text (str (-> notifier :config :domain) " 404: " path)
                          :channel "#docs-404-errors"
                          :username (-> notifier :config :username)})}}))
