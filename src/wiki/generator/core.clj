(ns wiki.generator.core
  (:require [wiki.generator.versions :as vgen]
            [wiki.generator.documents :as dgen]
            [wiki.generator.struct :refer [get-struct]]
            [wiki.generator.tree :as tree-gen]
            [wiki.generator.api-versions :as api-versions]
            [wiki.generator.git :as git]
            [wiki.generator.analysis.core :as analysis]
            [wiki.components.notifier :as notifications]
            [wiki.data.pages :as pdata]
            [wiki.data.folders :as fdata]
            [wiki.data.versions :as vdata]
            [wiki.data.pages :as pages-data]
            [wiki.web.redirects :as redirects]
            [wiki.util.utils :as utils]
            [playground-samples-parser.new.group-parser :as pgs]
            [wiki.components.offline-generator :refer [generate-zip]]
            [me.raynes.fs :as fs]
            [com.climate.claypoole :as cp]
            [taoensso.timbre :as timbre :refer [info error]]
            [version-clj.core :as version-clj]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as string]))


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


(defn complete-config [jdbc version version-config branch-path]
  (let [samples-count (dec (count (file-seq (clojure.java.io/file (str branch-path "/samples")))))
        articles-count (count (pages-data/pages-urls jdbc (:id version)))]
    (-> version-config
        (assoc-in [:vars :branch-name] (:key version))
        (assoc-in [:vars :samples-count] samples-count)
        (assoc-in [:vars :articles-count] articles-count))))


(defn generate-landing [jdbc version version-config branch-path]
  (let [landing-file-path (str branch-path "/index.html")
        landing-content (try
                          (slurp (str branch-path "/index.html"))
                          (catch Exception _ nil))
        version-config (complete-config jdbc version version-config branch-path)]
    (when landing-content
      (pdata/add-page jdbc
                      (:id version)
                      ""
                      "Landing"
                      (dgen/replace-vars landing-content (:vars version-config))
                      (git/file-last-commit-date branch-path (.getAbsolutePath (clojure.java.io/file landing-file-path)))
                      []
                      {}))))


(defn need-check-links [branch]
  (or (utils/released-version? (:name branch))
      (= (:name branch) "develop")
      (= (:name branch) "master")
      (string/includes? (:message branch) "#links")
      (string/includes? (:message branch) "#all")))


(defn generate-version
  [branch
   git-ssh
   api-versions
   docs-versions
   {:keys [jdbc notifier offline-generator] :as generator}
   {:keys [data-dir] :as generator-config}
   queue-index
   generate-images]
  (try
    (do
      (info "building" branch)
      (notifications/start-version-building notifier branch queue-index)
      (let [branch-path (str data-dir "/versions/" (:name branch))
            version-config (assoc-in (redirects/get-config (str branch-path "/config.toml"))
                                     [:vars :branch-name] (:name branch))
            samples (pgs/samples branch-path (:vars version-config))
            data (get-struct branch-path)
            tree (tree-gen/generate-tree data)
            version-id (vdata/add-version jdbc
                                          (:name branch)
                                          (:commit branch)
                                          tree
                                          version-config)
            version {:id  version-id
                     :key (:name branch)}]
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
                                        version
                                        samples
                                        data
                                        api-versions
                                        generator-config
                                        generate-images
                                        version-config)
                  *broken-link-result (promise)]

              (generate-landing jdbc version version-config branch-path)

              (when (and (:generate-images generator-config)
                         generate-images)
                (info "Uploading images: " (:images-dir generator-config) (:static-dir generator-config))
                (let [upl-res (sh "scp" "-r" (:images-dir generator-config) (:static-dir generator-config))]
                  (info "Uploading result: " upl-res)))

              (vgen/remove-previous-versions jdbc version-id (:name branch))

              (generate-zip offline-generator {:id  version-id
                                               :key (:name branch)})

              (if (need-check-links branch)
                (analysis/check-broken-links (:name branch) report *broken-link-result)
                (deliver *broken-link-result {:error-links report :check-broken-links-disabled true}))

              (let [total-report @*broken-link-result
                    conflicts-with-develop (if (= "develop" (:name branch))
                                             0
                                             (git/merge-conflicts git-ssh branch-path))]
                (timbre/info "Block until promise realised")
                (vdata/add-report jdbc version-id total-report)

                (notifications/complete-version-building notifier branch queue-index
                                                         report
                                                         conflicts-with-develop
                                                         (:broken-links total-report)))
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
                (notifications/build-failed notifier branch queue-index e)
                nil)))))
    (catch Exception e
      (do (error e)
          (error (.getMessage e))
          (notifications/build-failed notifier branch queue-index e)
          nil))))


(defn generate
  [{:keys                                                                            [jdbc notifier offline-generator] :as generator
    {:keys [show-branches git-ssh data-dir reference-versions] :as generator-config} :config}
   queue-index]
  (try
    (do
      (let [repo-path (str data-dir "/repo/")
            versions-path (str data-dir "/versions/")]
        (fs/mkdirs versions-path)
        (git/update-repo git-ssh repo-path)
        (let [actual-branches (vgen/actual-branches show-branches git-ssh repo-path)
              docs-versions (map :name actual-branches)
              removed-branches (vgen/remove-branches jdbc (map :name actual-branches) data-dir)
              branches (vgen/filter-for-rebuild jdbc actual-branches)
              name-branches (map :name branches)
              api-versions (api-versions/get-versions reference-versions)
              last-version (last-version jdbc name-branches)]
          (doall (pmap #(git/checkout git-ssh repo-path % (str versions-path %)) name-branches))
          (notifications/start-building notifier name-branches removed-branches queue-index)
          (info "api versions:" (pr-str api-versions))
          (info "last version:" last-version)
          ;(info "actual branches :" (pr-str actual-branches))
          (let [result (doall (map #(generate-version %
                                                      git-ssh
                                                      api-versions
                                                      docs-versions
                                                      generator
                                                      generator-config
                                                      queue-index
                                                      (= (:name %) last-version))
                                   branches))]
            (fs/delete-dir versions-path)
            (if (some nil? result)
              (notifications/complete-building-with-errors notifier name-branches queue-index)
              (notifications/complete-building notifier name-branches removed-branches queue-index))))))
    (catch Exception e
      (do (error e)
          (error (.getMessage e))
          (notifications/complete-building-with-errors notifier [] queue-index e)))))
