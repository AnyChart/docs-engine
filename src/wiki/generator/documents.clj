(ns wiki.generator.documents
  (:require [wiki.generator.markdown :as md]
            [wiki.data.pages :as pdata]
            [wiki.data.folders :as fdata]
            [taoensso.timbre :as timbre :refer [info]]
            [net.cgrand.enlive-html :as html]))

(defn- fix-url [url]
  (-> url
      (subs 1)
      (clojure.string/replace #"index\.md$" "")))

(defn- convert-content [content version-key playground api api-versions api-default-version]
  ;todo: test: (md/to-html (str "aa{tags} tag1, tag2 {tags}bb\n" content)  version-key playground api api-versions api-default-version)
  (md/to-html content version-key playground api api-versions api-default-version))

(defn- generate-struct-item
  [jdbc version base-path item api playground api-versions api-default-version]
  ;;(info "generating" (dissoc item :content))
  (if-let [content (:content item)]
    (let [{html :html tags :tags}
          (convert-content content (:key version) playground api api-versions api-default-version)]
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
        (doall (pmap #(generate-struct-item jdbc version (str base-path "/" (:name item))
                                            % api playground api-versions api-default-version)
                     items))))))

(defn generate [jdbc version data api playground api-versions api-default-version]
  (doall (pmap #(generate-struct-item jdbc version nil % api
                                      playground api-versions api-default-version)
              data)))
