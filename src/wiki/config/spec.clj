(ns wiki.config.spec
  (:require [clojure.spec.alpha :as s]))


(s/def :config.redis.spec/port pos-int?)
(s/def :config.redis.spec/host string?)
(s/def :config.redis.spec/db int?)
(s/def :config.redis/spec (s/keys :req-un [:config.redis.spec/port
                                           :config.redis.spec/host
                                           :config.redis.spec/db]))
(s/def :config/redis (s/keys :req-un [:config.redis/spec]))


(s/def :config.notifications.slack/channel string?)
(s/def :config.notifications.slack/token string?)
(s/def :config.notifications.slack/username string?)
(s/def :config.notifications/slack (s/keys :req-un [:config.notifications.slack/channel
                                                    :config.notifications.slack/token
                                                    :config.notifications.slack/username]))


(s/def :config.notifications.skype/id string?)
(s/def :config.notifications.skype/chat-id string?)
(s/def :config.notifications.skype/key string?)
(s/def :config.notifications/skype (s/keys :req-un [:config.notifications.skype/id
                                                    :config.notifications.skype/chat-id
                                                    :config.notifications.skype/key]))

(s/def :config/notifications (s/keys :req-un [:config.notifications/skype
                                              :config.notifications/slack]))

(s/def :config.indexer/queue string?)
(s/def :config/indexer (s/keys :req-un [:config.indexer/queue]))


(s/def :config.offline-generator/zip-dir string?)
(s/def :config.offline-generator/queue string?)
(s/def :config/offline-generator (s/keys :req-un [:config.offline-generator/zip-dir
                                                  :config.offline-generator/queue]))

(s/def :config.jdbc/subprotocol string?)
(s/def :config.jdbc/password string?)
(s/def :config.jdbc/classname string?)
(s/def :config.jdbc/subname string?)
(s/def :config.jdbc/user string?)
(s/def :config/jdbc (s/keys :req-un [:config.jdbc/subprotocol
                                     :config.jdbc/password
                                     :config.jdbc/classname
                                     :config.jdbc/subname
                                     :config.jdbc/user]))

(s/def :config.generator/reference-default-version string?)
(s/def :config.generator/max-processes pos-int?)
(s/def :config.generator/show-branches boolean?)
(s/def :config.generator/redirects-queue string?)
(s/def :config.generator/queue string?)
(s/def :config.generator/static-dir string?)
(s/def :config.generator/indexer-queue string?)
(s/def :config.generator/images-dir string?)
(s/def :config.generator/git-ssh string?)
(s/def :config.generator/data-dir string?)
(s/def :config.generator/reference-versions string?)
(s/def :config.generator/phantom-engine string?)
(s/def :config.generator/generator string?)
(s/def :config.generator/generate-images boolean?)
(s/def :config/generator (s/keys :req-un [:config.generator/reference-default-version
                                          :config.generator/max-processes
                                          :config.generator/show-branches
                                          :config.generator/redirects-queue
                                          :config.generator/queue
                                          :config.generator/static-dir
                                          :config.generator/indexer-queue
                                          :config.generator/images-dir
                                          :config.generator/git-ssh
                                          :config.generator/data-dir
                                          :config.generator/reference-versions
                                          :config.generator/phantom-engine
                                          :config.generator/generator
                                          :config.generator/generate-images]))


(s/def :config.web/redirects-queue string?)
(s/def :config.web/queue string?)
(s/def :config.web/static pos-int?)
(s/def :config.web/debug boolean?)
(s/def :config.web/port pos-int?)
(s/def :config.web/zip-queue string?)
(s/def :config.web/max-line pos-int?)
(s/def :config/web (s/keys :req-un [:config.web/redirects-queue
                                    :config.web/queue
                                    :config.web/static
                                    :config.web/debug
                                    :config.web/port
                                    :config.web/zip-queue
                                    :config.web/max-line]))


(s/def :config.sphinx/subprotocol string?)
(s/def :config.sphinx/subname string?)
(s/def :config.sphinx/table string?)
(s/def :config/sphinx (s/keys :req-un [:config.sphinx/subprotocol
                                       :config.sphinx/subname
                                       :config.sphinx/table]))


(s/def :config.common/prefix string?)
(s/def :config.common/domain string?)
(s/def :config.common/reference string?)
(s/def :config.common/playground string?)
(s/def :config.common/playground-project string?)
(s/def :config/common (s/keys :req-un [:config.common/prefix
                                       :config.common/domain
                                       :config.common/reference
                                       :config.common/playground
                                       :config.common/playground-project]))


(s/def ::config (s/keys :req-un [:config/common
                                 :config/sphinx
                                 :config/web
                                 :config/generator
                                 :config/jdbc
                                 :config/offline-generator
                                 :config/indexer
                                 :config/notifications
                                 :config/redis]))