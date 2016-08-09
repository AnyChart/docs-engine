(ns wiki.generator.documents
  (:require [wiki.generator.markdown :as md]
            [wiki.data.pages :as pdata]
            [wiki.data.folders :as fdata]
            [com.climate.claypoole :as cp]
            [taoensso.timbre :as timbre :refer [info]]
            [net.cgrand.enlive-html :as html]))

(defn- fix-url [url]
  (-> url
      (subs 1)
      (clojure.string/replace #"index\.md$" "")))

(defn- convert-content [content version-key pg-jdbc pg-version  playground api api-versions api-default-version]
  (md/to-html content version-key pg-jdbc pg-version  playground api api-versions api-default-version))

(defn- generate-struct-item
  [jdbc version pg-jdbc pg-version base-path item api playground api-versions api-default-version]
  ;;(info "generating" (dissoc item :content))
  (if-let [content (:content item)]
    (let [{html :html tags :tags}
          (convert-content content (:key version) pg-jdbc pg-version playground api api-versions api-default-version)]
      (pdata/add-page jdbc (:id version) (fix-url (str base-path "/"
                                                       (:name item)))
                      (:title item)
                      html
                      (:last-modified item)
                      tags))
    (let [items (:children item)]
      (when (seq items)
        (fdata/add-folder jdbc (:id version) (fix-url (str base-path "/" (:name item)))
                          (-> items first :name))
        (doall (map #(generate-struct-item jdbc version pg-jdbc pg-version
                                            (str base-path "/" (:name item))
                                            % api playground api-versions api-default-version)
                     items))))))

(defn generate [jdbc version pg-jdbc pg-version data api playground api-versions api-default-version]
  (cp/with-shutdown! [pool (+ 2 (cp/ncpus))]
                     (doall (cp/pmap pool #(generate-struct-item jdbc version
                                       pg-jdbc pg-version
                                       nil % api
                                       playground api-versions api-default-version)
                data))))
