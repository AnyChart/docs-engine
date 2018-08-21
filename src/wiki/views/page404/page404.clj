(ns wiki.views.page404.page404
  (:require [hiccup.page]
            [wiki.views.common :as common]
            [wiki.views.resources :as resources]))


(defn page [{:keys [commit] :as data}]
  (hiccup.page/html5
    {:lang "en"}
    (common/head data true)

    [:body
     resources/body-tag-manager
     (common/styles-body commit)
     [:header
      [:div.container-fluid
       [:div.row
        [:div.col-sm-24
         (common/anychart-brand)]
        (common/anychart-help)]]]

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
            [:i.glyphicon.glyphicon-search]]]]]]]]

     [:script {:type "text/javascript"} "window['version'] = 'latest';window['isUrlVersion']=false;"]
     [:script {:id "main_script" :type "text/javascript" :src (str "/main.min.js?v=" commit) :async true}]]))