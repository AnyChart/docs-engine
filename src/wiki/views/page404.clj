(ns wiki.views.page404
  (:require [hiccup.page]))

(defn page []
  (hiccup.page/html5
    {:lang "en"}
    [:head
     [:title "Not found | AnyChart Documentation"]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
     [:link {:rel "icon" :href "/i/anychart.ico" :type "image/x-icon"}]
     [:link {:rel "stylesheet" :type "text/css" :href "/main.css"}]
     [:link {:rel "stylesheet" :href "https://cdn.anychart.com/fonts/2.5.0/anychart.css"}]
     "<!--[if lt IE 9]>"
     [:script {:scr "https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"}]
     [:script {:scr "https://oss.maxcdn.com/libs/respond.js/1.3.0/respond.min.js"}]
     "<![endif]-->"
     [:link {:rel "apple-touch-icon", :sizes "57x57", :href "/icons/57.png"}]
     [:link {:rel "apple-touch-icon", :sizes "76x76", :href "/icons/76.png"}]
     [:link {:rel "apple-touch-icon", :sizes "120x120", :href "/icons/120.png"}]
     [:link {:rel "apple-touch-icon", :sizes "152x152", :href "/icons/152.png"}]
     [:link {:rel "apple-touch-icon", :sizes "167x167", :href "/icons/167.png"}]
     [:link {:rel "apple-touch-icon", :sizes "180x180", :href "/icons/180.png"}]
     [:script {:type "text/javascript"} "window['version'] = 'latest';window['isUrlVersion']=false;"]
     [:script {:id "main_script" :type "text/javascript" :src "/main.min.js" :async true}]]

    [:body

     [:header
      [:div.container-fluid
       [:div.row
        [:div.col-sm-24
         [:a.navbar-brand {:rel "nofollow" :href "https://www.anychart.com"}
          [:img {:alt "AnyChart" :src "/i/logo-empty.png"}]
          [:div.chart-row
           [:span.chart-col.green]
           [:span.chart-col.orange]
           [:span.chart-col.red]]]
         [:a.brand.hidden-super-small " Documentation"]]
        [:div.helpers.pull-right.hidden-mobile
         [:div.questions.affix
          [:a.text-support {:rel "nofollow" :href "http://support.anychart.com"}
           [:div]]
          [:span.hidden-super-small "Still have questions?"
           [:br]
           [:a {:rel "nofollow" :href "https://www.anychart.com/support/"}
            "Contact support"]]]]]]]

     [:div.wrapper.container-fluid
      [:div.row
       [:div.col-md-24
        [:div#article-content
         [:div.content404
          [:h1 "Error 404"]
          [:p "This page you were trying to reach at this address doesn't seem to exist.
This is usually the result of a bad or outdated link. We apologize for any inconvenience."]
          [:p "You can try:"
           [:ul
            [:li "Start with "
             [:a {:href "/Quick_Start/Quick_Start"} "Quick Start"]]
            [:li "Search "
             [:a {:href "/"} "docs.anychart.com"] ":"]]
           [:div.search404.inner-addon.hidden-mobile
            [:input.form-control.input-sm {:placeholder "What are you looking for?" :type "text"}]
            [:i.glyphicon.glyphicon-search]]]]]]]]]))