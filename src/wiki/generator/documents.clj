(ns wiki.generator.documents
  (:require [wiki.generator.markdown :as md]
            [wiki.data.pages :as pdata]
            [wiki.data.folders :as fdata]
            [com.climate.claypoole :as cp]
            [taoensso.timbre :as timbre :refer [info]]
            [net.cgrand.enlive-html :as html]
            [wiki.generator.analysis.core :as analyzer]))

(defn- fix-url [url]
  (-> url
      (subs 1)
      (clojure.string/replace #"index\.md$" "")))

(defn- generate-struct-item
  [notifier jdbc version samples base-path item api-versions generator-config generate-images report]
  ;;(info "generating" (dissoc item :content))
  (if-let [content (:content item)]
    (let [page-url (fix-url (str base-path "/" (:name item)))
          page-report (atom (analyzer/start content))
          {html :html tags :tags}
          (md/to-html notifier page-url content (:key version) samples api-versions generator-config generate-images page-report)]
      (pdata/add-page jdbc (:id version) page-url
                      (:title item)
                      html
                      (:last-modified item)
                      tags
                      (:config item))
      ;(prn "report: " (:title item) (:name item) @page-report)
      (when (not-empty @page-report)
        (swap! report conj (assoc @page-report :page-url page-url))))
    (let [items (:children item)]
      (when (seq items)
        (fdata/add-folder jdbc (:id version) (fix-url (str base-path "/" (:name item)))
                          (-> items first :name))
        (doall (map #(generate-struct-item notifier jdbc version samples
                                           (str base-path "/" (:name item))
                                           % api-versions generator-config generate-images report)
                    items))))))

(defn generate [notifier jdbc version samples data api-versions
                generator-config generate-images]
  (let [report (atom [])]
    (cp/with-shutdown! [pool (+ 2 (cp/ncpus))]
                      (doall (cp/pmap pool #(generate-struct-item notifier jdbc version
                                                                  samples nil % api-versions
                                                                  generator-config generate-images report)
                                      data)))
    @report))
