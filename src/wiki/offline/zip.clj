(ns wiki.offline.zip
  (import [java.util.zip ZipEntry ZipOutputStream])
  (:require [clojure.java.io :as io]))

(defn get-entry-name [folder-path file-path prefix]
  (str prefix (subs file-path (count folder-path))))

(defn zip-folder [folder zip-path prefix]
  (with-open [zip (ZipOutputStream. (io/output-stream zip-path))]
    (doseq [f (file-seq (io/file folder)) :when (.isFile f)]
      (.putNextEntry zip (ZipEntry. (get-entry-name folder (.getPath f) prefix)))
      (io/copy f zip)
      (.closeEntry zip))))