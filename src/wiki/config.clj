(ns wiki.config)

(def base-path (if (System/getProperty "dev")
                 "/Users/alex/Work/anychart/docs-engine"
                 "/apps/wiki"))
(def data-path (str base-path "/data"))
(def config (if (System/getProperty "dev")
              {:show-branches true
               :playground "playground.anychart.dev/acdvf-docs/"}
              {:git "/apps/wiki/keys/git"
               :show-branches (not (System/getProperty "prod"))
               :playground "playground.anychart.com/acdvf-docs/"}))
(def repo-path (str data-path "/repo"))
(def versions-path (str data-path "/versions"))
