(ns wiki.generator.core
  (:require [wiki.generator.versions :as vgen]
            [wiki.generator.documents :as dgen]
            [wiki.generator.struct :refer [get-struct]]
            [wiki.generator.tree :as tree-gen]
            [wiki.components.notifier :as notifications]
            [wiki.data.versions :as vdata]
            [wiki.generator.api-versions :as api-versions]
            [taoensso.timbre :as timbre :refer [info error]]))

(defn- generate-version
  [branch jdbc notifier git-ssh data-dir api playground api-versions api-default-version]
  (try
    (do
      (info "building" branch)
      (notifications/start-version-building notifier (:name branch))
      (let [data (get-struct (str data-dir "/versions/" (:name branch)))
            tree (tree-gen/generate-tree data)
            version-id (vdata/add-version jdbc
                                          (:name branch)
                                          (:commit branch)
                                          tree)]
        (try
          (do
            (dgen/generate jdbc {:id version-id
                                 :key (:name branch)}
                           data api playground api-versions api-default-version)
            (notifications/complete-version-building notifier (:name branch))
            (vgen/remove-previous-versions jdbc version-id (:name branch)))
          (catch Exception e
            (do (error e)
                (error (.getMessage e))
                (if version-id
                  (vdata/delete-by-id jdbc version-id))
                (notifications/build-failed notifier (:name branch)))))))
    (catch Exception e
      (do (error e)
          (error (.getMessage e))
          (notifications/build-failed notifier (:name branch))))))

(defn generate
  [jdbc notifier
   {:keys [show-branches git-ssh data-dir reference playground
           reference-versions reference-default-version]}]
  (notifications/start-building notifier)
  (let [actual-branches (vgen/update-branches show-branches git-ssh data-dir)
        removed-branches (vgen/remove-branches jdbc (map :name actual-branches))
        branches (vgen/filter-for-rebuild jdbc actual-branches)
        api-versions (api-versions/get-versions reference-versions)]
    (if (seq branches)
      (notifications/versions-for-build notifier (map :name branches)))
    (if (seq removed-branches)
      (notifications/delete-branches notifier removed-branches))
    (info "api versions:" api-versions)
    (info "api default version:" reference-default-version)
    (doall (map #(generate-version %
                                   jdbc
                                   notifier
                                   git-ssh
                                   data-dir
                                   reference
                                   playground
                                   api-versions
                                   reference-default-version)
                branches))
    (notifications/complete-building notifier)))