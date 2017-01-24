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

(defn- generate-struct-item
  [notifier jdbc version samples base-path item api playground api-versions api-default-version]
  ;;(info "generating" (dissoc item :content))
  (if-let [content (:content item)]
    (let [page-url (fix-url (str base-path "/" (:name item)))
          {html :html tags :tags}
          (md/to-html notifier page-url content (:key version) samples playground api api-versions api-default-version)]
      (pdata/add-page jdbc (:id version) page-url
                      (:title item)
                      html
                      (:last-modified item)
                      tags
                      (:config item)))
    (let [items (:children item)]
      (when (seq items)
        (fdata/add-folder jdbc (:id version) (fix-url (str base-path "/" (:name item)))
                          (-> items first :name))
        (doall (map #(generate-struct-item notifier jdbc version samples
                                            (str base-path "/" (:name item))
                                            % api playground api-versions api-default-version)
                     items))))))

(defn generate [notifier jdbc version samples data api playground api-versions api-default-version]
  (cp/with-shutdown! [pool (+ 2 (cp/ncpus))]
                     (doall (cp/pmap pool #(generate-struct-item notifier jdbc version
                                                                 samples nil % api
                                                                 playground api-versions api-default-version)
                data))))
