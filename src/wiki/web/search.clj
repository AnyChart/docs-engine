(ns wiki.web.search
  (:require [wiki.data.search :as search]
            [wiki.web.helpers :refer :all]
            [wiki.util.utils :as utils]
            [hiccup.core :as hiccup]
            [clojure.string :as string]))


(defn- format-search-result [result query version]
  (let [words (string/split query #" ")
        url (:url result)
        title (reduce (fn [res q]
                        (string/replace res
                                        (re-pattern (str "(?i)" (string/re-quote-replacement q)))
                                        #(str "{b}" % "{eb}")))
                      (:url result) words)
        title (-> title
                  (string/replace #"_" " ")
                  (string/split #"/"))
        title (map #(-> %
                        (string/replace #"\{b\}" "<span class='match'>")
                        (string/replace #"\{eb\}" "</span>"))
                   title)]
    (assoc result
      :title (str (string/join " / " (drop-last 1 title))
                  " / <a href='./" (utils/escape-url url) "'>" (last title) "</a>"))))


(defn search-results [request version]
  (map #(format-search-result % (-> request :params :q) (:key version))
       (search/search-for (sphinx request)
                          (-> request :params :q)
                          (:id version)
                          (:key version)
                          (-> request :component :sphinx :config :table))))


(defn search-result-to-html [res query]
  [:div#article-content
   [:div#search-content
    [:button {:type "button" :class "btn btn-default btn-white btn-sm visible-xs"}
     [:i {:class "ac ac-arrow-left-thin"}] " Back"]
    [:h1.search
     [:button {:type "button" :class "btn btn-default btn-white btn-sm hidden-xs"}
      [:i {:class "ac ac-arrow-left-thin"}] " Back"]
     "Search results for " [:span query]]
    (if (seq res)
      (for [item res]
        [:div.result-block
         [:h2 (:title item)]
         [:p (:sn item)]])
      "Nothing found")]])


(defn search-results-html [request version query]
  (hiccup/html
    (search-result-to-html
      (search-results request version) query)))