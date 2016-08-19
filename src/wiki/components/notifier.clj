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

(defn- notify-raw [notifier text]
  (http/post (str "https://anychart-team.slack.com/services/hooks/incoming-webhook?token="
                  (-> notifier :config :token))
             {:form-params
              {:payload (generate-string
                          {:text text
                           :channel (-> notifier :config :channel)
                           :username (-> notifier :config :username)})}}))

(defn- add-domain [notifier text]
  (str (-> notifier :config :domain) " " text))

(defn- notify-simple [notifier text]
  (notify-raw notifier (add-domain notifier text)))

(defn notify-delay [notifier message]
  (send (:messages notifier) conj message))

(defn notify [notifier message]
  (send (:messages notifier) (fn [messages message]
                              (let [all-msg (map #(add-domain notifier %) (conj messages message))
                                    total-msg (clojure.string/join "\n" all-msg)]
                                (notify-raw notifier total-msg))
                              []) message))

(defn delete-branches [notifier branches]
  (notify notifier (str "branches deleted: " (clojure.string/join ", " branches))))

(defn start-building [notifier]
  (notify notifier "start docs update"))

(defn complete-building [notifier]
  (notify notifier "docs update completed"))

(defn versions-for-build [notifier versions]
  (notify notifier (str "build list: " (clojure.string/join ", " versions))))

(defn start-version-building [notifier version]
  (notify notifier (str version " start building ")))

(defn complete-version-building [notifier version]
  (notify notifier (str version " build completed")))

(defn build-failed [notifier version]
  (notify notifier (str version " build FAILED")))

(defn sample-parsing-error [notifier version page-url]
  (notify-delay notifier (str version " sample parsing error: " page-url)))

(defn image-format-error [notifier version page-url]
  (notify-delay notifier (str version " image format error: " page-url)))

(defn sample-not-available [notifier version page-url url]
  (notify-delay notifier (str version " sample not available " page-url " - " url)))

(defn notify-404 [notifier path]
  (http/post (str "https://anychart-team.slack.com/services/hooks/incoming-webhook?token="
                  (-> notifier :config :token))
             {:form-params
              {:payload (generate-string
                         {:text (str (-> notifier :config :domain) " 404: " path)
                          :channel "#docs-404-errors"
                          :username (-> notifier :config :username)})}}))
