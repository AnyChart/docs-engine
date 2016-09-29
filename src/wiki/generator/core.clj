(ns wiki.generator.core
  (:require [wiki.generator.versions :as vgen]
            [wiki.generator.documents :as dgen]
            [wiki.generator.struct :refer [get-struct]]
            [wiki.generator.tree :as tree-gen]
            [wiki.components.notifier :as notifications]
            [wiki.data.pages :as pdata]
            [wiki.data.folders :as fdata]
            [wiki.data.versions :as vdata]
            [wiki.data.playground :as pg-data]
            [wiki.generator.api-versions :as api-versions]
            [playground-samples-parser.fs :as pgs]
            [wiki.components.offline-generator :refer [generate-zip]]
            [me.raynes.fs :as fs]
            [com.climate.claypoole :as cp]
            [taoensso.timbre :as timbre :refer [info error]]))

(defn- generate-version
  [branch jdbc notifier offline-generator data-dir api playground api-versions api-default-version queue-index]
  (try
    (do
      (info "building" branch)
      (notifications/start-version-building notifier (:name branch) queue-index)
      (let [branch-path (str data-dir "/versions/" (:name branch))
            samples (pgs/samples branch-path)
            data (get-struct branch-path)
            tree (tree-gen/generate-tree data)
            version-id (vdata/add-version jdbc
                                          (:name branch)
                                          (:commit branch)
                                          tree)]
        (try
          (let [static-branch-dir (str data-dir "/static/" (:name branch))
                images-branch-dir (str branch-path "/images")]
            (when (fs/exists? images-branch-dir)
              (fs/mkdirs static-branch-dir)
              (fs/copy-dir-into images-branch-dir static-branch-dir)))
          (do
            (dgen/generate notifier
                           jdbc {:id  version-id
                                 :key (:name branch)}
                           samples
                           data api playground api-versions api-default-version)
            (vgen/remove-previous-versions jdbc version-id (:name branch))
            (generate-zip offline-generator {:id  version-id
                                             :key (:name branch)})
            (notifications/complete-version-building notifier (:name branch) queue-index)
            version-id)
          (catch Exception e
            (do (error e)
                (error (.getMessage e))
                (when version-id
                  ; wait for pmap threads terminated
                  (Thread/sleep 500)
                  (pdata/delete-version-pages jdbc version-id)
                  (fdata/delete-version-folders jdbc version-id)
                  (vdata/delete-by-id jdbc version-id))
                (notifications/build-failed notifier (:name branch) queue-index)
                nil)))))
    (catch Exception e
      (do (error e)
          (error (.getMessage e))
          (notifications/build-failed notifier (:name branch) queue-index)
          nil))))

(defn generate
  [jdbc notifier offline-generator
   {:keys [show-branches git-ssh data-dir reference playground
           reference-versions reference-default-version]} queue-index]
  (fs/mkdirs (str data-dir "/versions"))
  (let [actual-branches (vgen/update-branches show-branches git-ssh data-dir)
        removed-branches (vgen/remove-branches jdbc (map :name actual-branches) data-dir)
        branches (vgen/filter-for-rebuild jdbc actual-branches)
        name-branches (map :name branches)
        api-versions (api-versions/get-versions reference-versions)]
    (notifications/start-building notifier name-branches removed-branches queue-index)
    (info "api versions:" api-versions)
    (info "api default version:" reference-default-version)
    (let [result (doall (map #(generate-version %
                                                jdbc
                                                notifier
                                                offline-generator
                                                data-dir
                                                reference
                                                playground
                                                api-versions
                                                reference-default-version
                                                queue-index)
                             branches))]
      (fs/delete-dir (str data-dir "/versions"))
      (if (some nil? result)
        (notifications/complete-building-with-errors notifier name-branches queue-index)
        (notifications/complete-building notifier name-branches removed-branches queue-index)))))
