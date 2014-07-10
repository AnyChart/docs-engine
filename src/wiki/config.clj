(ns wiki.config)

(def base-path (if (System/getProperty "dev")
                 "/Users/alex/Work/anychart/wiki"
                 "/apps/wiki"))
(def data-path (str base-path "/data"))
(def config (read-string (slurp (str base-path "/config"))))
(def repo-path (str data-path "/repo"))
(def versions-path (str data-path "/versions"))
