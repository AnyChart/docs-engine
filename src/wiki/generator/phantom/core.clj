(ns wiki.generator.phantom.core
  (:require [clojure.java.io :refer [file]]
            [clojure.string :refer [lower-case]]
            [clojure.java.shell :refer [sh]]
            [wiki.generator.phantom.download :as download]
            [wiki.util.utils :as utils]
            [selmer.parser :refer [render-file]]
            [taoensso.timbre :as timbre :refer [info]])
  (:import [org.imgscalr Scalr Scalr$Method Scalr$Mode]
           [java.awt.image BufferedImageOp BufferedImage]
           [javax.imageio ImageIO]))


(defn- fix-code [code]
  (clojure.string/replace code
                          (clojure.string/re-quote-replacement ".animation(true")
                          ".animation(false"))


(defn generate-img [phantom-engine
                    phantom-generator
                    images-folder
                    page-url
                    version
                    sample]
  (info "generate-img:" page-url phantom-engine phantom-generator version images-folder sample)
  (let [code (render-file "templates/phantom.selmer"
                          {:scripts (download/get-urls (:scripts sample))
                           :styles  (download/get-urls (:styles sample))
                           :code    (fix-code (:code sample))
                           :style   (:style sample)
                           :markup  (:markup sample)})
        tmp-file (java.io.File/createTempFile "sample" ".html")
        image-path (str images-folder "/" (utils/name->url page-url) ".png")
        *sh-result (volatile! nil)]
    (info "generating" (.getAbsolutePath tmp-file) "for" (:name sample) "to" image-path)
    (with-open [f (clojure.java.io/writer tmp-file)]
      (binding [*out* f]
        (println code)))
    (try
      (do
        (let [sh-result (sh phantom-engine
                            "--web-security=false"
                            phantom-generator
                            (.getAbsolutePath tmp-file)
                            image-path
                            "'chart draw'")]
          (vreset! *sh-result sh-result))
        (let [image (ImageIO/read (file image-path))
              res (Scalr/resize image
                                Scalr$Method/ULTRA_QUALITY
                                Scalr$Mode/FIT_TO_WIDTH
                                (* 2 310)
                                (* 2 150)
                                (into-array BufferedImageOp [Scalr/OP_ANTIALIAS]))]
          (ImageIO/write res "png" (file image-path)))
        (sh "pngquant" "--force" "--ext" ".png" image-path)
        (info "generated" image-path)
        (.delete tmp-file)
        nil)
      (catch Exception e
        (do
          (timbre/error "generation failed for" (:url sample) "html:" (.getAbsolutePath tmp-file) @*sh-result e)
          {:sample (:url sample) :code (.getAbsolutePath tmp-file)})))))


