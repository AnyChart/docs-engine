(ns wiki.generator.analysis.page
  (:require [hiccup.page :as hiccup-page]))


(defn drop-base-path [url version-key]
  (let [index (.indexOf url version-key)
        url (if (>= index 0)
              (subs url (+ index (count version-key)))
              url)
        url (if (.startsWith url "/")
              (subs url 1)
              url)
        url (if (= url "") "/" url)]
    url))


(defn page [{:keys [broken-links error-links] :as report}
            version-key]
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
      [:h3 "Pages with errors"]
      (if (empty? error-links)
        [:p {:style "color: green;"} "Not found."]
        (for [link (map-indexed #(assoc %2 :id (inc %1)) error-links)]
          [:p {:style "padding: 5px 0;"}
           [:div [:span (str (:id link) ". ")] (:page-url link)]
           [:table (for [[k v] link]
                     (when (and (not= k :page-url)
                                (not= k :id))
                       [:tr
                        [:th (name k)]
                        [:td (for [bad-link v]
                               [:div bad-link])]]))]]))

      [:h3 "Pages with 404 links"]
      (if (:check-broken-links-disabled report)
        [:p {:style "color: gray;"} "Checker not started. Use flags #links or #all in a commit message.<br>Always ON in develop, master and release version."]
        (if (empty? broken-links)
          [:p {:style "color: green;"} "Not found."]
          [:table {:cell-padding   10
                   :cell-spacing   10
                   :border-spacing "5px 15px"
                   ;:border 1
                   :style          "padding: 5px; margin: 10px;"}
           [:tr
            [:th "№"]
            [:th "URL"]
            [:th "broken links (href - text)"]
            ;[:th "404"]
            ]
           (for [item (map-indexed #(assoc %2 :id (inc %1))
                                   (sort-by :url broken-links))]
             [:tr
              [:td (:id item)]
              [:td {:style "padding-right: 5px;"}
               [:a {:href (:url item)}
                (drop-base-path (:url item) version-key)]]
              [:td
               (for [link (:links item)]
                 [:div
                  [:a {:href (:bad-url item)} (:href link)]
                  [:span {:style "color: #AAAAAA;"} " - "]
                  [:span (:text link)]]
                 )]
              ;[:td (:bad-url link)]
              ])]))]]))