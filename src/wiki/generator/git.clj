(ns wiki.generator.git
  (:require [clojure.java.shell :refer [sh with-sh-env with-sh-dir]]
            [clojure.string :refer [split]]))


(defn run-sh [& command]
  (apply sh command))


(defn file-last-commit-date [base-path path]
  (with-sh-dir base-path
               (let [res (sh "git" "--no-pager" "log" "-1" "--format=%ct" "--" path)]
                 (-> res
                     :out
                     (clojure.string/trim)
                     read-string))))


(defn- run-git [git-ssh path & command]
  (with-sh-env {:GIT_SSH git-ssh}
               (with-sh-dir path
                            (let [res (apply sh "/usr/bin/git" command)]
                              (println command res)
                              (:out res)))))


(defn current-commit [git-ssh path]
  (subs (run-git git-ssh path "rev-parse" "HEAD") 0 7))


(defn set-user [git-ssh path]
  (run-git git-ssh path "config" "user.name" "docs-engine")
  (run-git git-ssh path "config" "user.email" "support@anychart.com"))


(defn update-repo [git-ssh repo]
  (run-git git-ssh repo "fetch" "-p"))


(defn checkout [git-ssh repo version target-path]
  (run-sh "rm" "-rf" target-path)
  (run-sh "cp" "-r" repo target-path)
  (run-git git-ssh target-path "checkout" version)
  (run-git git-ssh target-path "pull" "origin" version))


(defn merge-conflicts [git-ssh path]
  (set-user git-ssh path)
  (run-git git-ssh path "checkout" "develop")
  (run-git git-ssh path "pull" "origin" "develop")
  (run-git git-ssh path "checkout" (last (clojure.string/split path #"/")))
  (let [git-resp (run-git git-ssh path "merge" "--no-commit" "--no-ff" "develop")
        result (count (re-seq #"CONFLICT" git-resp))]
    result))


(defn remote-branches [git-ssh path pred]
  (let [raw-lines (split (run-git git-ssh path "branch" "-r" "--format='%(refname:short)|-|%(objectname)|-|%(authorname)|-|%(contents:subject)'") #"\n")
        lines (map #(clojure.string/replace % #"'" "") raw-lines)
        filtered-lines (filter (fn [s] (and (some? s)
                                            (not (.contains s "origin/HEAD"))
                                            (pred s))) lines)
        branches (map (fn [s]
                        (let [[raw-name commit author message] (clojure.string/split s #"\|-\|")]
                          {:name    (last (re-matches #"origin/(.+)" raw-name))
                           :commit  commit
                           :author  author
                           :message message})) filtered-lines)]
    branches))


(defn actual-branches-with-hashes [git-ssh path]
  (remote-branches git-ssh path (constantly true)))


(defn version-branches-with-hashes [git-ssh path]
  (remote-branches git-ssh path (fn [s] (re-find #"origin/\d+\.\d+\.\d+" s))))
