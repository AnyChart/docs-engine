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
            [taoensso.timbre :as timbre :refer [info error]]
            [wiki.util.utils :as utils]
            [version-clj.core :as version-clj]
            [clojure.java.shell :refer [sh]]))

(defn last-version? [jdbc branch-name]
  (let [last-version (vdata/default jdbc)]
    (and (utils/released-version? branch-name)
         (or
           (= 0 (version-clj/version-compare branch-name last-version))
           (= 1 (version-clj/version-compare branch-name last-version))))))

(defn last-version [jdbc branches]
  (let [versions (vdata/versions jdbc)]
    (first (sort (comp - version-clj/version-compare)
                 (concat versions branches)))))

(defn- generate-version
  [branch
   api-versions
   {:keys [jdbc notifier offline-generator] :as generator}
   {:keys [data-dir] :as generator-config}
   queue-index
   generate-images]
  (try
    (do
      (info "building" branch)
      (notifications/start-version-building notifier (:name branch) queue-index)
      (let [branch-path (str data-dir "/versions/" (:name branch))
            samples (pgs/samples branch-path)
            [data redirect-data] (get-struct branch-path)
            tree (tree-gen/generate-tree data)
            version-id (vdata/add-version jdbc
                                          (:name branch)
                                          (:commit branch)
                                          tree
                                          {:redirects redirect-data})]
        (try
          (let [static-branch-dir (str data-dir "/static/" (:name branch))
                images-branch-dir (str branch-path "/images")]
            (when (fs/exists? images-branch-dir)
              (fs/mkdirs static-branch-dir)
              (fs/copy-dir-into images-branch-dir static-branch-dir)))
          (do
            (when (and (:generate-images generator-config)
                       generate-images)
              (fs/delete-dir (:images-dir generator-config))
              (fs/mkdirs (:images-dir generator-config)))
            (let [report (dgen/generate notifier
                                        jdbc
                                        {:id  version-id
                                         :key (:name branch)}
                                        samples
                                        data
                                        api-versions
                                        generator-config
                                        generate-images)]
              (vdata/add-report jdbc version-id report)
              (when (and (:generate-images generator-config)
                         generate-images)
                (info "Uploading images: " (:images-dir generator-config) (:static-dir generator-config))
                (let [upl-res (sh "scp" "-r" (:images-dir generator-config) (:static-dir generator-config))]
                  (info "Uploading result: " upl-res)))
              (vgen/remove-previous-versions jdbc version-id (:name branch))
              (generate-zip offline-generator {:id  version-id
                                               :key (:name branch)})
              (notifications/complete-version-building notifier (:name branch) queue-index report)
              version-id))
          (catch Exception e
            (do (error e)
                (error (.getMessage e))
                (when version-id
                  ; wait for pmap threads terminated
                  (Thread/sleep 500)
                  (pdata/delete-version-pages jdbc version-id)
                  (fdata/delete-version-folders jdbc version-id)
                  (vdata/delete-by-id jdbc version-id))
                (notifications/build-failed notifier (:name branch) queue-index e)
                nil)))))
    (catch Exception e
      (do (error e)
          (error (.getMessage e))
          (notifications/build-failed notifier (:name branch) queue-index e)
          nil))))

(defn generate
  [{:keys [jdbc notifier offline-generator] :as generator
    {:keys [show-branches git-ssh data-dir reference-versions] :as generator-config} :config}
   queue-index]
  (try
    (do
      (fs/mkdirs (str data-dir "/versions"))
      (let [actual-branches (vgen/update-branches show-branches git-ssh data-dir)
            removed-branches (vgen/remove-branches jdbc (map :name actual-branches) data-dir)
            branches (vgen/filter-for-rebuild jdbc actual-branches)
            name-branches (map :name branches)
            api-versions (api-versions/get-versions reference-versions)
            last-version (last-version jdbc name-branches)]
        (notifications/start-building notifier name-branches removed-branches queue-index)
        (info "api versions:" api-versions)
        (info "last version:" last-version)
        (let [result (doall (map #(generate-version %
                                                    api-versions
                                                    generator
                                                    generator-config
                                                    queue-index
                                                    (= (:name %) last-version))
                                 branches))]
          (fs/delete-dir (str data-dir "/versions"))
          (if (some nil? result)
            (notifications/complete-building-with-errors notifier name-branches queue-index)
            (notifications/complete-building notifier name-branches removed-branches queue-index)))))
    (catch Exception e
      (do (error e)
          (error (.getMessage e))
          (notifications/complete-building-with-errors notifier [] queue-index e)))))
