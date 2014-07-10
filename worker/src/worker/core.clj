(ns worker.core
  (:require [clojure.java.shell :refer [sh with-sh-env with-sh-dir]]
            [clojure.string :refer [split trim]]
            [clojure.java.io :refer [file]]
            [taoensso.carmine :as car :refer (wcar)])
  (:gen-class :main :true))

(def config {:repo "git@github.com:AnyChart/docs.git"
             :git_ssh "/apps/wiki/keys/git"
             :out "/wiki"})

(def extra-config (read-string (slurp "/apps/wiki/config")))
(def show-branches (:show_branches extra-config))

(def data-path (str (:out config) "/data"))
(def repo-path (str (:out config) "/repo"))
(def env-config {:GIT_SSH (:git_ssh config)})

(def redis-conn {:pool {} :spec {:host "localhost"
                                 :port 6379}})

(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

(defn run-sh [& command]
  (with-sh-env env-config
    (apply sh command)))

(defn run-git [& command]
  (with-sh-env env-config
    (with-sh-dir repo-path
      (let [res (apply sh "/usr/bin/git" command)]
        (prn res)
        res))))

(defn clone-project []
  (run-sh "/usr/bin/git" "clone" (:repo config) repo-path))

(defn update-project []
  (run-git "checkout" "master")
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
  (run-git "pull" "origin" ref)
  (let [out (str data-path "/" ref)]
    (run-sh "rm" "-rf" out)
    (run-sh "cp" "-R" repo-path out)))

(defn rebuild-structure []
  (println "Rebuilding wiki...")
  (update-project)
  (doseq [item (if show-branches
                 (concat (get-tags) (get-branches-for-build))
                 (get-tags))]
    (build-ref item))
  (println "Done!")
  (wcar* (car/publish "docs" "rebuild")))

(defn -main [& args]
  (if (not (= 0 (:exit (check-repo))))
    (System/exit -1))
  (if (not (path-exists data-path))
    (.mkdir (file data-path)))
  (println "Repository updated")
  (println "Listening redis for rebuild signal")
  (car/with-new-pubsub-listener (:spec redis-conn)
    {"docs" (fn [msg] (rebuild-structure))}
    (car/subscribe "docs")))

