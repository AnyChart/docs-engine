(ns wiki.generator.documents
  (:require [wiki.generator.markdown :as md]
            [wiki.data.pages :as pdata]
            [wiki.data.folders :as fdata]
            [taoensso.timbre :as timbre :refer [info]]))

(defn- fix-url [url]
  (-> url
      (subs 1)
      (clojure.string/replace #"index\.md$" "")))

(defn- generate-struct-item [jdbc version base-path item api playground]
  (info "generating" (dissoc item :content))
  (if-let [content (:content item)]
    (pdata/add-page jdbc (:id version) (fix-url (str base-path "/"
                                                     (:name item)))
                    (:title item)
                    (md/to-html content (:key version) playground api))
    (let [items (:children item)]
      (when (seq items)
        (fdata/add-folder jdbc (:id version) (fix-url (str base-path "/" (:name item)))
                          (-> items first :name))
        (doall (map #(generate-struct-item jdbc version (str base-path "/" (:name item))
                                           % api playground)
                    items))))))

(defn generate [jdbc version data api playground]
  (doall (map #(generate-struct-item jdbc version nil % api playground)
              data)))
