(ns wiki.generator.analysis.page
  (:require [hiccup.page :as hiccup-page]
            [wiki.generator.analysis.core :as analysis]
            ))


(defn page [{:keys [broken-links error-links]}
            version-key]
  (let [broken-links (analysis/format-report broken-links)]
    (hiccup-page/html5
      {:lang "en"}
      [:head
       [:meta {:charset "UTF-8"}]
       [:meta {:content "IE=edge" :http-equiv "X-UA-Compatible"}]
       [:meta {:content "width=device-width, initial-scale=1" :name "viewport"}]
       [:title "AnyChart Docs Report"]
       "<!--[if lt IE 9]><script src=\"https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js\"></script><script src=\"https://oss.maxcdn.com/respond/1.4.2/respond.min.js\"></script><![endif]-->"
       "<!-- Latest compiled and minified CSS and Optional theme-->"
       [:link {:href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" :rel "stylesheet"}]
       [:link {:href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css" :rel "stylesheet"}]
       [:script {:src "https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"}]
       "<!-- Latest compiled and minified JavaScript -->"
       [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"}]
       [:style "th, td { padding: 1px 3px; }
       table, th, td { border: 1px solid #DDDDDD;}"]]

      [:body
       [:div.container
        [:h2 "Report for version " [:b version-key] " "
         [:small
          [:a {:href (str "/" version-key "/report.json")} "json version"]]]
        [:h3 "Pages with bad links"]
        (for [link (map-indexed #(assoc %2 :id (inc %1)) error-links)]
          [:p {:style "padding: 5px 0;"}
           [:div [:span (str (:id link) ". ")] (:page-url link)]
           [:table (for [[k v] link]
                     (when (and (not= k :page-url)
                                (not= k :id))
                       [:tr
                        [:th (name k)]
                        [:td (for [bad-link v]
                               [:div bad-link])]]))]])

        [:h3 "Pages with 404 links"]
        [:table {:cell-padding   10
                 :cell-spacing   10
                 :border-spacing "5px 15px"
                 ;:border 1
                 :style          "padding: 5px; margin: 10px;"}
         [:tr
          [:th "№"]
          [:th "URL"]
          [:th "broken hrefs"]
          ;[:th "404"]
          ]
         (for [link (map-indexed #(assoc %2 :id (inc %1)) broken-links)]
           [:tr
            [:td (:id link)]
            [:td {:style "padding-right: 5px;"}
             [:a {:href (:url link)} (:url link)]]
            [:td
             (for [href (:hrefs link)]
               [:div [:a {:href (:bad-url link)} href]])]
            ;[:td (:bad-url link)]
            ])]]])))