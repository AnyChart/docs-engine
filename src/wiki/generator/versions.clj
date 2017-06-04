(ns wiki.generator.versions
  (:require [wiki.generator.git :as git]
            [wiki.data.versions :as vdata]
            [wiki.data.pages :as pdata]
            [wiki.data.folders :as fdata]
            [wiki.components.notifier :as notifications]
            [clojure.java.shell :refer [sh]]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as timbre :refer [info error]]))

(defn actual-branches [show-branches git-ssh repo-path]
  (if show-branches
    (git/actual-branches-with-hashes git-ssh repo-path)
    (git/version-branches-with-hashes git-ssh repo-path)))

(defn- remove-branch [jdbc branch-key]
  (let [version-id (:id (vdata/version-by-key jdbc branch-key))]
    (pdata/delete-version-pages jdbc version-id)
    (fdata/delete-version-folders jdbc version-id)
    (vdata/delete-by-id jdbc version-id)))

(defn remove-branches [jdbc actual-branches data-dir]
  (info "actual branches" (vec actual-branches))
  (let [current-branches (vdata/versions jdbc)
        removed-branches (filter #(not (some #{%} actual-branches)) current-branches)]
    (info "current branches" (vec current-branches))
    (info "removed branches" (vec removed-branches))
    (if (seq removed-branches)
      (doseq [branch-key removed-branches]
        (fs/delete-dir (str data-dir "/static/" branch-key))
        (remove-branch jdbc branch-key)))
    removed-branches))

(defn filter-for-rebuild [jdbc branches]
  (filter #(vdata/need-rebuild? jdbc (:name %) (:commit %)) branches))

(defn remove-previous-versions [jdbc actual-id key]
  (let [ids (vdata/version-ids jdbc key)
        outdated-ids (filter #(not= actual-id %) ids)]
    (doall (map (fn [vid]
                  (pdata/delete-version-pages jdbc vid)
                  (fdata/delete-version-folders jdbc vid)
                  (vdata/delete-by-id jdbc vid))
                outdated-ids))))
