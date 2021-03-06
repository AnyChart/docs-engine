(ns wiki.components.notifier
  (:require [com.stuartsierra.component :as component]
            [cheshire.core :refer [generate-string]]
            [org.httpkit.client :as http]
            [wiki.notification.slack :as slack]
            [wiki.notification.skype :as skype]
            [clojure.string :as string]))


(defrecord Notifier [config]
  component/Lifecycle
  (start [this] this)
  (stop [this] this))


(defn new-notifier [config]
  (map->Notifier {:config config :messages (agent [])}))


(defn start-building [notifier branches removed-branches queue-index]
  (slack/start-building notifier branches removed-branches queue-index)
  ;(skype/start-building notifier branches removed-branches queue-index)
  )


(defn complete-building [notifier branches removed-branches queue-index]
  (slack/complete-building notifier branches removed-branches queue-index)
  ;(skype/complete-building notifier branches removed-branches queue-index)
  )


(defn complete-building-with-errors [notifier branches queue-index & [e]]
  (slack/complete-building-with-errors notifier branches queue-index e)
  (skype/complete-building-with-errors notifier branches queue-index e))


(defn start-version-building [notifier branch queue-index]
  (slack/start-version-building notifier (:name branch) queue-index)
  (skype/start-version-building notifier branch queue-index))


(defn complete-version-building [notifier {version :name :as branch} queue-index
                                 {broken-links :broken-links error-links :error-links :as report}
                                 conflicts-with-develop]
  (let [direct-links (count (set (mapcat :direct-links error-links)))
        canonical-links (count (set (mapcat :non-canonical-links error-links)))
        http-links (count (set (mapcat :http-links error-links)))
        env-links (count (set (mapcat :env-links error-links)))
        sample-not-available (count (set (mapcat :sample-not-available error-links)))
        sample-parsing-error (count (set (mapcat :sample-parsing-error error-links)))
        image-format-error (count (set (mapcat :image-format-error error-links)))
        toc-error (count (set (mapcat :toc-error error-links)))
        broken-links-error (count broken-links)
        msg-coll [(when (pos? direct-links) (str "Direct links: " direct-links))
                  (when (pos? canonical-links) (str "Non canonical links: " canonical-links))
                  (when (pos? http-links) (str "Http links: " http-links))
                  (when (pos? env-links) (str "Env links: " env-links))
                  (when (pos? sample-not-available) (str "Unavailable samples: " sample-not-available))
                  (when (pos? sample-parsing-error) (str "Parsing samples errors: " sample-parsing-error))
                  (when (pos? image-format-error) (str "Parsing images errors: " image-format-error))
                  (when (pos? toc-error) (str "TOC errors: " toc-error))
                  (when (pos? conflicts-with-develop) (str "Conflicts with develop: " conflicts-with-develop))
                  (when (pos? broken-links-error) (str "404 errors: " broken-links-error))]
        msg (string/join "\n" (filter some? msg-coll))]
    (slack/complete-version-building notifier version queue-index)
    (if (= 0 direct-links canonical-links env-links http-links
           sample-not-available sample-parsing-error image-format-error conflicts-with-develop toc-error broken-links-error)
      (skype/complete-version-building notifier branch queue-index report)
      (skype/complete-version-building-with-warnings notifier branch queue-index report msg))))


(defn build-failed [notifier branch queue-index & [e]]
  (slack/build-failed notifier (:name branch) queue-index e)
  (skype/build-failed notifier branch queue-index e))


(defn sample-parsing-error [notifier version page-url]
  (slack/sample-parsing-error notifier version page-url))


(defn image-format-error [notifier version page-url]
  (slack/image-format-error notifier version page-url))


(defn sample-not-available [notifier version page-url]
  (slack/sample-not-available notifier version page-url))


(defn notify-404 [notifier path]
  (slack/notify-404 notifier path))
