(ns wiki.views.main
  (:require [clojure.java.io :as io]
            [hiccup.core :as h]
            [hiccup.page]
            [clojure.string :as s]
            [clj-time.core :as t]
            [selmer.util :as selmer-utils]))

(def samples-script (slurp (io/resource "templates/samples-update.selmer")))
(def ga-script (slurp (io/resource "templates/google-analytics.selmer")))

;(def ga (selmer-utils/resource-path "templates/samples-update.selmer"))

(defn escape-url [str]
  (s/escape str {\% "%25"}))

(defn tree-view [el version is-url-version]
  (let [url (if is-url-version
              (str "/" version (escape-url (:url el)))
              (escape-url (:url el)))]
    (if (contains? el :children)
      (str "<li>"
           "<a href='" url "'><i class='folder-open'>+ </i>" (:title el) "</a>"
           "<ul>" (reduce str (map #(tree-view % version is-url-version) (:children el))) "</ul>"
           "</li>")
      (str "<li><a href='" url "'>" (:title el) "</a></li>"))))

(defn tree [data]
  (let [entries (:tree data)]
    (reduce str (map #(tree-view % (:version data) (:is-url-version data)) entries))))


(defn head [data]
  [:head
   [:title (:title-prefix data)]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   [:meta {:name "description" :content (:description data)}]

   [:meta {:property "og:title" :content (:title-prefix data)}]
   [:meta {:property "og:description" :content (:description data)}]
   [:meta {:property "og:image" :content (str "http://static.anychart.com/docs/images/" (:image-url data) ".png")}]
   [:meta {:property "og:type" :content "article"}]
   [:meta {:property "og:site_name" :content "AnyChart Documentation"}]
   [:meta {:property "article:publisher" :content "https://www.facebook.com/AnyCharts"}]
   [:meta {:property "fb:admins" :content "704106090"}]
   (when (seq (-> data :page :tags))
     [:meta {:name "keywords" :content (s/join ", " (-> data :page :tags))}])
   [:meta {:content "c5fb5d43a81ea360" :name "yandex-verification"}]
   [:link {:rel "canonical" :href (str "https://docs.anychart.com"
                                       (when (:url data) (str "/" (:url data))))}]
   [:link {:type "image/x-icon" :href "/i/anychart.ico" :rel "icon"}]
   "<!--[if IE]>"
   [:link {:rel "stylesheet" :type "text/css" :href (str "/main.css?v=" (:commit data))}]
   [:link {:rel "stylesheet" :type "text/css" :href (:anychart-css-url data)}]
   ; http://cdn.anychart.com/fonts/2.7.5/demo.html
   [:link {:rel "stylesheet" :type "text/css" :href "https://cdn.anychart.com/fonts/2.7.2/anychart.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "https://fonts.googleapis.com/css?family=Open+Sans:400,600"}]
   ;[:link {:rel "stylesheet" :type "text/css" :href "/lib/jquery-custom-content-scroller/jquery.mCustomScrollbar.min.css"}]
   [:link {:rel "apple-touch-icon" :sizes "57x57" :href "/icons/57.png"}]
   [:link {:rel "apple-touch-icon" :sizes "76x76" :href "/icons/76.png"}]
   [:link {:rel "apple-touch-icon" :sizes "120x120" :href "/icons/120.png"}]
   [:link {:rel "apple-touch-icon" :sizes "120x120" :href "/icons/152.png"}]
   [:link {:rel "apple-touch-icon" :sizes "152x152" :href "/icons/167.png"}]
   [:link {:rel "apple-touch-icon" :sizes "167x167" :href "/icons/180.png"}]
   "<![endif]-->"
   "<!--[if lt IE 9]>"
   [:script {:scr "https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"}]
   [:script {:scr "https://oss.maxcdn.com/libs/respond.js/1.3.0/respond.min.js"}]
   "<![endif]-->"
   [:script#anychart_script {:async "true" :src (:anychart-url data)}]])


(defn header [data]
  [:header
   [:div.container-fluid
    [:div.row

     [:div.col-sm-24
      [:a.navbar-brand {:title  "AnyChart Home"
                        :target "_blank"
                        :rel    "nofollow"
                        :href   "https://www.anychart.com"}
       ;[:img {:alt "AnyChart" :src "/i/logo-empty.png"}]
       [:div.border-icon]
       [:div.chart-row
        [:span.chart-col.green]
        [:span.chart-col.orange]
        [:span.chart-col.red]]]
      [:span.brand-label
       [:a.brand {:title  "AnyChart Home"
                  :target "_blank"
                  :rel    "nofollow"
                  :href   "https://www.anychart.com"} "AnyChart"]
       [:span.hidden-extra-mobile " Documentation"]]

      [:div.dropdown.pull-right.version-select.hidden-tablet
       [:button.btn.btn-blue.btn-sm {:data-toggle "dropdown" :type "button"}
        [:span.version-label (str "Version " (:version data))]
        [:span.caret]]
       [:ul.dropdown-menu.version-menu {:role "menu"}
        (for [v (:versions data)]
          [:li [:a {:href (:url v)} (str "Version " (:key v))]])]]

      [:div.visible-tablet.pull-right
       [:a.sidebar-switcher
        [:i.ac.ac-bars]
        [:i.ac.ac-remove]]]
      ;; without this chrome sometimes puts sidebar-switcher on new line
      [:div {:style "display: inline-block;"} ""]]

     [:div.helpers.pull-right.hidden-tablet
      [:div.questions.affix
       [:a.text-support {:title "AnyChart Support"
                         :rel   "nofollow"
                         :href  "http://support.anychart.com"}
        [:div]]
       [:span.hidden-super-small "Still have questions?"
        [:br]
        [:a {:title "AnyChart Support"
             :rel   "nofollow"
             :href  "https://www.anychart.com/support/"}
         "Contact support"]]]]]]])


(defn mobile-search [data]
  [:div.mobile-search-container.visible-tablet
   [:div.container-fluid
    [:div.row
     [:div.col-xs-24
      [:div.search.inner-addon.right-addon
       ;[:i.ac.ac-search]
       [:input.form-control.input-sm {:placeholder "What are you looking for?" :type "text"}]
       [:i.glyphicon.glyphicon-search]]]]]])


(defn main-content [data]
  [:div.wrapper.container-fluid
   [:div.row

    [:div.left-sidebar-container.hidden-tablet
     [:div.white-shadow.visible-tablet]
     [:div.sidebar

      ;; search inputh
      [:div.search.inner-addon.hidden-tablet
       [:input.form-control.input-sm {:placeholder "What are you looking for?" :type "text"}]
       [:i.glyphicon.glyphicon-search]]

      ;; version select dropdown
      [:div.buttons-container.visible-tablet
       [:div.dropdown.version-select
        [:button.btn.btn-white.btn-sm {:data-toggle "dropdown" :type "button"}
         [:span.version-label (str "Version " (:version data))]
         [:span.caret]]
        [:ul.dropdown-menu.version-menu {:role "menu"}
         (for [v (:versions data)]
           [:li [:a {:href (:url v)} (str "Version " (:key v))]])]]

       ;; support link
       [:div.questions.pull-right
        [:a.text-support {:title "AnyChart Support"
                          :rel   "nofollow"
                          :href  "http://support.anychart.com"}
         [:div]]
        [:span.hidden-super-small "Still have questions?"
         [:br]
         [:a {:title "AnyChart Support"
              :rel   "nofollow"
              :href  "https://www.anychart.com/support/"}
          "Contact support"]]]]

      [:ul.menu
       (tree data)]
      [:div.footer
       [:div.footer-inner
        [:a.soc-network
         {:title  "AnyChart Facebook"
          :target "_blank"
          :rel    "nofollow"
          :href   "https://www.facebook.com/AnyCharts"}
         [:span.soc-network-icon.fb [:i.sn-mini-icon.ac.ac-facebook]]]
        [:a.soc-network
         {:title  "AnyChart Twitter"
          :target "_blank"
          :rel    "nofollow"
          :href   "https://twitter.com/AnyChart"}
         [:span.soc-network-icon.tw [:i.sn-mini-icon.ac.ac-twitter]]]
        [:a.soc-network
         {:title  "AnyChart LinkedIn"
          :target "_blank"
          :rel    "nofollow"
          :href   "https://www.linkedin.com/company/386660"}
         [:span.soc-network-icon.in [:i.sn-mini-icon.ac.ac-linkedin]]]
        [:p (str "© " (t/year (t/now)) " ")
         [:a {:href   "https://www.anychart.com"
              :rel    "nofollow"
              :target "_blank"} "AnyChart.Com"] " All rights reserved."]]]]]

    [:div#page-content.col-md-24
     [:div#article-content
      (-> data :page :content)]]

    [:div.pull-right.warning-container
     (when (:old data)
       [:div#warning.warning-version.alert.alert-default.fade.in
        [:button.close {:aria-hidden "true" :data-dismiss "alert" :type "button"} "×"]
        [:i.ac.ac-exclamation]
        (str " You are looking at an outdated " (:version data) " version of this document. Switch to the ")
        [:a {:href (:old-url data) :data-last-version "latest"}
         (:actual-version data)]
        " version to see the up to date information."])]

    [:div.right-sidebar-container.pull-right.hidden-sm.hidden-xs.hidden-mobile
     [:div.right-bar-side
      [:div#table-of-content-large]
      [:div.right-buttons
       [:a {:href     (str "/" (:version data) "/download")
            :rel      "nofollow"
            :title    "Get offline version"
            :download true}
        [:div.icon.get-offline]
        [:span "Get offline version"]]

       [:a {:href   "https://github.com/anychart/anychart/issues"
            :rel    "nofollow"
            :target "_blank"
            :title  "Report issue"}
        [:div.icon.report2]
        [:span "Report issue"]]

       [:a {:href   "https://github.com/AnyChart/docs.anychart.com#overview"
            :rel    "nofollow"
            :target "_blank"
            :title  "Edit this page"}
        [:div.icon.edit]
        [:span "Edit this page"]]]]]]])


(defn body [data]
  [:body
   [:link {:rel "stylesheet" :type "text/css" :href (str "/main.css?v=" (:commit data))}]
   [:link {:rel "stylesheet" :type "text/css" :href (:anychart-css-url data)}]
   [:link {:rel "stylesheet" :type "text/css" :href "https://cdn.anychart.com/fonts/2.7.2/anychart.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "https://fonts.googleapis.com/css?family=Open+Sans:400,600"}]
   ;[:link {:rel "stylesheet" :type "text/css" :href "/lib/jquery-custom-content-scroller/jquery.mCustomScrollbar.min.css"}]
   [:link {:rel "apple-touch-icon" :sizes "57x57" :href "/icons/57.png"}]
   [:link {:rel "apple-touch-icon" :sizes "76x76" :href "/icons/76.png"}]
   [:link {:rel "apple-touch-icon" :sizes "120x120" :href "/icons/120.png"}]
   [:link {:rel "apple-touch-icon" :sizes "120x120" :href "/icons/152.png"}]
   [:link {:rel "apple-touch-icon" :sizes "152x152" :href "/icons/167.png"}]
   [:link {:rel "apple-touch-icon" :sizes "167x167" :href "/icons/180.png"}]
   (header data)
   (mobile-search data)
   (main-content data)

   [:script {:type "text/javascript"}
    (str
      "window['version'] = '" (:version data) "';
       window['isUrlVersion'] = " (boolean (:is-url-version data)) ";"
      "if (location.pathname != encodeURI('/" (:version data) "/' + '" (:url data) "') &&
              location.pathname != encodeURI('/" (:url data) "')  &&  window.history){
         window.history.replaceState(null, null, encodeURI('/" (:url data) "'));}"
      )]
   [:script {:id "main_script" :type "text/javascript" :src (str "/main.min.js?v=" (:commit data)) :async true}]

   samples-script
   ;(when-not (:is-ga-speed-insights data) ga-script)
   ])


(defn page [data]
  (hiccup.page/html5
    {:lang "en"}
    (head data)
    (body data)))
