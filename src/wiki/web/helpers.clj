(ns wiki.web.helpers)

(defn config [request]
  (-> request :component :config))

(defn jdbc [request]
  (-> request :component :jdbc))

(defn sphinx [request]
  (-> request :component :sphinx))

(defn redis [request]
  (-> request :component :redis))

(defn notifier [request]
  (-> request :component :notifier))

(defn offline-generator [request]
  (-> request :component :offline-generator))