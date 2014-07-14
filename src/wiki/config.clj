(ns wiki.config)

(def base-path (if (System/getProperty "dev")
                 "/Users/alex/Work/anychart/docs.anychart.com"
                 "/apps/wiki"))
(def data-path (str base-path "/data"))
(def config (if (System/getProperty "dev")
              {:git "/Users/alex/Work/anychart/docs.anychart.com/keys/git"
               :show-branches true
               :playground "playground.anychart.dev/acdvf-docs/"}
              {:git "/apps/wiki/keys/git"
               :show-branches true
               :playground "playground.anychart.dev/acdvf-docs/"}))
(def repo-path (str data-path "/repo"))
(def versions-path (str data-path "/versions"))
