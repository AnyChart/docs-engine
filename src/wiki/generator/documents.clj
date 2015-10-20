(ns wiki.generator.documents
  (:require [wiki.generator.markdown :as md]
            [wiki.data.pages :as pdata]
            [taoensso.timbre :as timbre :refer [info]]))

(defn- generate-struct-item [jdbc version base-path item api playground]
  (info "generating" (dissoc item :content))
  (if-let [content (:content item)]
    (pdata/add-page jdbc (:id version) (str base-path "/" (:name item)) (:title item)
                    (md/to-html content (:key version) playground api))
    (if-let [items (:children item)]
      (doall (map #(generate-struct-item jdbc version (str base-path "/" (:name item))
                                         % api playground)
                  items)))))

(defn generate [jdbc version data api playground]
  (doall (map #(generate-struct-item jdbc version nil % api playground)
              data)))
