(ns wiki.generator
  (:require [wiki.git :as git]
            [wiki.data.versions :as vdata]
            [wiki.data.pages :as pdata]
            [wiki.data.sitemap :as sitemap]
            [wiki.components.notifier :as notifications]
            [taoensso.timbre :as timbre :refer [info error]]))

(defn- update-branches [show-branches git-ssh data-dir]
  (let [repo-path (str data-dir "/repo/")
        versions-path (str data-dir "/versions/")]
    (git/update git-ssh repo-path)
    (let [branches (if show-branches
                     (git/actual-branches git-ssh repo-path)
                     (git/version-branches git-ssh repo-path))]
      (doall (pmap #(git/checkout git-ssh repo-path % (str versions-path %)) branches))
      (git/get-hashes git-ssh versions-path branches))))

(defn- remove-branch [jdbc branch-key]
  (let [version-id (:id (vdata/version-by-key jdbc branch-key))]
    (pdata/delete-version-pages jdbc version-id)
    (sitemap/remove-by-version jdbc version-id)
    (vdata/delete-by-id jdbc version-id)))

(defn- remove-branches [jdbc actual-branches]
  (info "actual branches" (vec actual-branches))
  (let [current-branches (vdata/versions jdbc)
        removed-branches (filter #(not (some #{%} actual-branches)) current-branches)]
    (info "current branches" (vec current-branches))
    (info "removed branches" (vec removed-branches))
    (if (seq removed-branches)
      (doall (map #(remove-branch jdbc %) removed-branches)))
    removed-branches))

(defn- filter-for-rebuild [jdbc branches]
  (filter #(vdata/need-rebuild? jdbc (:name %) (:commit %)) branches))

(defn- remove-previous-versions [jdbc actual-id key]
  (let [ids (vdata/version-ids jdbc key)
        outdated-ids (filter #(not= actual-id %) ids)]
    (doall (map (fn [vid]
                  (pdata/delete-version-pages jdbc vid)
                  (sitemap/remove-by-version jdbc vid)
                  (vdata/delete-by-id jdbc vid))
                outdated-ids))))

(defn- build-branch
  [branch jdbc notifier git-ssh data-dir max-processes jsdoc-bin docs playground]
  (try
    (do
      (info "building" branch)
      (notifications/start-version-building notifier (:name branch))
      ;; TODO: implement
      (notifications/complete-version-building notifier (:name branch)))
    (catch Exception e
      (do (error e)
          (error (.getMessage e))
          (notifications/build-failed notifier (:name branch))))))

(defn build-all [jdbc notifier config])
