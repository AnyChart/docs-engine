(ns worker.core
  (:require [clojure.edn :as edn]
            [clojure.java.shell :refer [sh with-sh-env with-sh-dir]]
            [clojure.string :refer [split trim]]
            [clojure.java.io :refer [file]]
            [taoensso.carmine :as car :refer (wcar)])
  (:gen-class))

(def config (:worker (edn/read-string (slurp "/app/config.clj"))))
(def data-path (str (:out config) "/data"))
(def repo-path (str (:out config) "/repo"))
(def env-config {:GIT_SSH (:git_ssh config)})

(def env (System/getenv))

(def redis-conn {:pool {} :spec {:host (get env "REDIS_PORT_6379_TCP_ADDR")
                                 :port (read-string (get env "REDIS_PORT_6379_TCP_PORT"))}})

(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

(defn run-sh [& command]
  (with-sh-env env-config
    (apply sh command)))

(defn run-git [& command]
  (with-sh-env env-config
    (with-sh-dir repo-path
      (apply sh "/usr/bin/git" command))))

(defn clone-project []
  (run-sh "/usr/bin/git" "clone" (:repo config) repo-path))

(defn update-project []
  (run-git "pull")
  (run-git "fetch" "--tags"))

(defn path-exists [path]
  (.exists (file path)))

(defn get-tags []
  (split (:out (run-git "tag")) #"\s"))

(defn get-branches-for-build []
  (map (fn [s] (re-find #"master|develop" s))
                      (filter (fn [s] (and (not (.contains s "->"))
                                           (re-matches #"[ ]+origin/(master|develop)" s)))
                              (split (:out (run-git "branch" "-r")) #"\n"))))

(defn check-repo []
  (if (path-exists repo-path)
    (update-project)
    (clone-project)))

(defn build-ref [ref]
  (prn (str "Building " ref))
  (run-git "checkout" ref)
  (let [out (str data-path "/")]
    (run-sh "rm" "-rf" out)
    (run-sh "cp" "-R" out)))

(defn rebuild-structure []
  (prn "Rebuilding wiki...")
  (update-project)
  (doseq [item (concat (get-tags) (get-branches-for-build))]
    (build-ref item))
  (prn "Done!"))

(defn -main [& args]
  (if (not (= 0 (:exit (check-repo))))
    (System/exit -1))
  (if (not (path-exists data-path))
    (.mkdir (file data-path)))
  (prn "Repository updated")
  (prn "Listening redis for rebuild signal")
  (car/with-new-pubsub-listener (:spec redis-conn)
    {"docs" (fn [msg] (rebuild-structure))}
    (car/subscribe "docs")))

