(ns wiki.views.main.main-page
  (:require [wiki.util.utils :as utils]
            [wiki.views.common :as common]
            [wiki.views.resources :as resources]
            [wiki.config.core :as c]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [hiccup.core :as h]
            [hiccup.page :as hiccup-page]
            [clj-time.core :as t]))


(def samples-script (slurp (io/resource "templates/samples-update.selmer")))
(def ga-script (slurp (io/resource "templates/google-analytics.selmer")))

;(def ga (selmer-utils/resource-path "templates/samples-update.selmer"))


(defn tree-view [el version is-url-version]
  (let [url (if is-url-version
              (str "/" version (utils/escape-url (:url el)))
              (utils/escape-url (:url el)))]
    (if (contains? el :children)
      (str "<li>"
           "<a href='" url "'><i class='folder-open'>+ </i>" (:title el) "</a>"
           "<ul>" (reduce str (map #(tree-view % version is-url-version) (:children el))) "</ul>"
           "</li>")
      (str "<li><a href='" url "'>" (:title el) "</a></li>"))))


(defn tree [data]
  (let [entries (:tree data)]
    (reduce str (map #(tree-view % (:version data) (:is-url-version data)) entries))))


(defn header [data]
  [:header
   [:div.container-fluid
    [:div.row
     [:div.col-sm-24
      (common/anychart-brand)

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

     (common/anychart-help)]]])


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

       ;[:a {:href     (str "/" (:version data) "/download")
       ;     :rel      "nofollow"
       ;     :title    "Get offline version"
       ;     :download true}
       ; [:div.icon.get-offline]
       ; [:span "Get offline version"]]

       [:a {:href   "https://github.com/anychart/anychart/issues"
            :rel    "nofollow"
            :target "_blank"
            :title  "Report issue"}
        [:div.icon.report2]
        [:span "Report issue"]]

       [:a {:href   "https://github.com/AnyChart/anychart-bible#overview"
            :rel    "nofollow"
            :target "_blank"
            :title  "Edit this page"}
        [:div.icon.edit]
        [:span "Edit this page"]]]]]]])


(defn body [data]
  [:body
   resources/body-tag-manager
   (common/styles-body (:commit data))
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
  (hiccup-page/html5
    {:lang "en"}
    (common/head data true)
    (body data)))
