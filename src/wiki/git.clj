(ns wiki.git
  (:require [clojure.java.shell :refer [sh with-sh-env with-sh-dir]]
            [clojure.java.io :refer [file writer]]
            [clojure.tools.logging :as log]
            [clojure.string :refer [split trim]]
            [wiki.config :refer [config]]))

(def env-config (if (:git config)
                  {:GIT_SSH (:git config)}))

(defn run-sh [& command]
  (with-sh-env env-config
    (log/info "running shell:" command)
    (let [res (apply sh command)]
      (log/info "result:" res)
      res)))

(defn run-git [path & command]
  (with-sh-env env-config
    (with-sh-dir path
      (log/info "running git" command)
      (let [res (apply sh "/usr/bin/git" command)]
        (log/info "response:" res)
        (:out res)))))

(defn lock-path [path]
  (let [f (file path)]
    (str (.getParent f) "/" (.getName f) ".lock")))

(defn lock [path]
  (log/info "lock" path)
  (with-open [w (writer (lock-path path))]
    (.write w "locked")))

(defn unlock [path]
  (log/info "unlock" path)
  (.delete (file (lock-path path))))

(defn locked? [path]
  (.exists (file (lock-path path))))

(defn do-update [path]
  (lock path)
  ; should test lock. probably not working correctly :(
  ; or work in main thread, terrible terrible bug
  (let [res (future (str (run-git path "fetch")))]
    @res
    (unlock path)
    "Updated"))

(defn update [path]
  (if (locked? path)
    (do (log/info path "locked")
        "Repository locked")
    (do-update path)))

(defn actual-branches [path]
  (log/info "branches:" (split (run-git path "branch" "-r") #"\n"))
  (map (fn [s] (last (re-matches #".*origin/([^ ]+).*" s)))
       (filter (fn [s] (and (not (= s nil))
                            (not (.contains s "->"))))
               (split (run-git path "branch" "-r") #"\n"))))

(defn version-branches [path]
  (map (fn [s] (re-find #"\d\.\d\.\d" s))
       (filter (fn [s] (and (not (.contains s "->"))
                            (re-matches #"[ ]+origin/\d\.\d\.\d" s)))
               (split (run-git path "branch" "-r") #"\n"))))

(defn checkout-to [path branch target]
  (run-sh "cp" "-R" path target)
  (run-git target "checkout" branch)
  (run-git target "pull" "origin" branch))
