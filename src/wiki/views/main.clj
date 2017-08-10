(ns wiki.views.main
  (:require [hiccup.core :as h]
            [hiccup.page]
            [clojure.string :as s]))

(defn escape-url [str]
  (s/escape str {\% "%25"}))

(defn tree-view [el version is-url-version]
  (let [url (if is-url-version
              (str "/" version (escape-url (:url el)))
              (escape-url (:url el)))]
    (if (contains? el :children)
      (str "<li>"
           ;"<a href='" url "'><i class='c ac-folder-open'></i> " (:title el) "</a>"
           "<a href='" url "'><i class='folder-open'>+ </i><b>" (:title el) "</b></a>"
           "<ul>" (reduce str (map #(tree-view % version is-url-version) (:children el))) "</ul>"
           "</li>")
      ;(str "<li> <a href='" url "'><i class='ac ac-file-text'></i> " (:title el) "</a></li>")
      (str "<li><a href='" url "'>" (:title el) "</a></li>")
      )))

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
   [:meta {:property "og:image" :content "http://static.anychart.com/docs/images/{{image-url}}.png"}]
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
   [:link {:rel "stylesheet" :type "text/css" :href "/main.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href (:anychart-css-url data)}]
   [:link {:rel "stylesheet" :type "text/css" :href "https://cdn.anychart.com/fonts/2.7.2/anychart.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "https://fonts.googleapis.com/css?family=Open+Sans:400,600"}]
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

     ;:div.col-lg-22.col-lg-offset-1

     ;[:a.sidebar-switcher.hidden-md.hidden-lg [:i.ac.ac-bars]]

     [:div.col-lg-15.col-lg-offset-3
      [:a.navbar-brand {:rel "nofollow" :href "https://www.anychart.com"}
       [:img {:alt "AnyChart" :height "72" :width "300" :src "/i/logo-empty.png"}]
       [:div.chart-row
        [:span.chart-col.green]
        [:span.chart-col.orange]
        [:span.chart-col.red]]]
      [:a.brand.hidden-super-small " Documentation"]

      [:div.dropdown.pull-right.version-select
       [:button.btn.btn-blue.btn-sm {:data-toggle "dropdown" :type "button"}
        [:span.version-label (str "Version " (:version data))]
        [:span.caret]]
       [:ul.dropdown-menu.version-menu {:role "menu"}
        (for [v (:versions data)]
          [:li [:a {:href (:url v)} (str "Version " (:key v))]])]]
      ]

     [:div.helpers.hidden-830

      ;[:div.dropdown
      ; [:button.btn.btn-blue.btn-sm {:data-toggle "dropdown" :type "button"}
      ;  [:span.version-label (str "Version " (:version data))]
      ;  [:span.caret]]
      ; [:ul.dropdown-menu.version-menu {:role "menu"}
      ;  (for [v (:versions data)]
      ;    [:li [:a {:href (:url v)} (str "Version " (:key v))]])]]

      ;[:div.dropdown
      ; [:button.btn.btn-primary {:data-toggle "dropdown" :type "button"}
      ;  [:span.version-label (str "Version " (:version data))]
      ;  [:span.caret]]
      ; [:ul.dropdown-menu.version-menu {:role "menu"}
      ;  (for [v (:versions data)]
      ;    [:li [:a {:href (:url v)} (str "Version " (:key v))]])]]

      ;[:div.btn-group
      ; {:style "padding-left: 15px;"}
      ; [:button.btn.btn-default.btn-zip
      ;  {:type "button" :role "button" :onclick (str "location.href='/" (:version data) "/download'")}
      ;  [:span.ac.ac-download-zip {:aria-hidden "true"}]
      ;  "Download .zip"]]

      [:div.text-muted.questions
       [:a.text-support {:rel "nofollow" :href "http://support.anychart.com"}
        ;[:i.ac.ac-support]
        [:img {:src "/svg/support.svg" :width "27px" :style "color=white"}]
        ]
       [:span.hidden-super-small "Still have questions?"
        [:br]
        [:a {:rel "nofollow" :href "https://www.anychart.com/support/"}
         " Contact support"]]]

      ]]]

   ]
  )

(defn body [data]
  [:body
   [:link {:rel "stylesheet" :type "text/css" :href "/main.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href (:anychart-css-url data)}]
   [:link {:rel "stylesheet" :type "text/css" :href "https://cdn.anychart.com/fonts/2.7.2/anychart.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "https://fonts.googleapis.com/css?family=Open+Sans:400,600"}]
   [:link {:rel "apple-touch-icon" :sizes "57x57" :href "/icons/57.png"}]
   [:link {:rel "apple-touch-icon" :sizes "76x76" :href "/icons/76.png"}]
   [:link {:rel "apple-touch-icon" :sizes "120x120" :href "/icons/120.png"}]
   [:link {:rel "apple-touch-icon" :sizes "120x120" :href "/icons/152.png"}]
   [:link {:rel "apple-touch-icon" :sizes "152x152" :href "/icons/167.png"}]
   [:link {:rel "apple-touch-icon" :sizes "167x167" :href "/icons/180.png"}]
   (header data)
   ]
  )



(defn mobile-menu [data]
  [:div#bar
   [:div.helpers.visible-830.bordered
    [:div.btn-group
     [:button.btn.btn-default.btn-blue.dropdown-toggle
      {:data-toggle "dropdown" :type "button"}
      [:span.version-label (str "Version " (:version data))]
      [:span.caret]]
     [:ul.dropdown-menu.version-menu {:role "menu"}
      (for [version (:versions data)]
        [:ul [:li [:a {:href (:url version)} (str "Version " (:key version))]]])]]
    [:div.text-muted.questions
     [:a.text-support
      {:rel "nofollow" :href "http://support.anychart.com"}
      [:i.ac.ac-support]]
     [:span.hidden-super-small " Still have questions?"
      [:br] " "
      [:a {:rel "nofollow" :href "https://www.anychart.com/support/"} " Contact support"]]]]
   [:ul.menu
    (tree data)]
   [:div.footer
    [:div.footer-inner
     [:a.soc-network
      {:target "_blank" :rel "nofollow" :href "https://www.facebook.com/AnyCharts"}
      [:span.soc-network-icon.fb [:i.sn-mini-icon.ac.ac-facebook]]]
     [:a.soc-network
      {:target "_blank" :rel "nofollow" :href "https://twitter.com/AnyChart"}
      [:span.soc-network-icon.tw [:i.sn-mini-icon.ac.ac-twitter]]]
     [:a.soc-network
      {:target "_blank" :rel "nofollow" :href "https://www.linkedin.com/company/386660"}
      [:span.soc-network-icon.in [:i.sn-mini-icon.ac.ac-linkedin]]]
     [:p " © 2017 AnyChart.Com All rights reserved."]]]]
  )


(defn main-content [data]
  [:div.wrapper.container-fluid
   [:div.row

    [:div#bar-side.col-md-8.col-lg-6.col-lg-offset-3.hidden-xs.hidden-sm
     [:div.row.hidden-xs.hidden-sm                          ;.search-large-screen
      [:div.col-md-24.col-lg-24

       [:div.inner-addon.right-addon
        [:i.glyphicon.glyphicon-search]
        ;[:i.ac.ac-search]
        [:input.form-control  {:placeholder "What are you looking for?" :type "text"}]

        ;[:span.input-group-btn
        ; [:button.btn.btn-default
        ;  {:type "button"}
        ;  [:span.ac.ac-search.form-control-feedback]]]

        ]]]
     [:ul.menu
      (tree data)]
     [:div.footer
      [:div.footer-inner
       [:a.soc-network
        {:target "_blank" :rel "nofollow" :href "https://www.facebook.com/AnyCharts"}
        [:span.soc-network-icon.fb [:i.sn-mini-icon.ac.ac-facebook]]]
       [:a.soc-network
        {:target "_blank" :rel "nofollow" :href "https://twitter.com/AnyChart"}
        [:span.soc-network-icon.tw [:i.sn-mini-icon.ac.ac-twitter]]]
       [:a.soc-network
        {:target "_blank" :rel "nofollow" :href "https://www.linkedin.com/company/386660"}
        [:span.soc-network-icon.in [:i.sn-mini-icon.ac.ac-linkedin]]]
       [:p " © 2017 AnyChart.Com All rights reserved."]]]]

    [:div.row.helpers.hidden-lg.hidden-md
     [:div.col-xs-24.col-sm-12
      [:div.input-group.search
       [:input.form-control
        {:placeholder "What are you looking for?" :type "text"}]
       [:span.input-group-btn
        [:button.btn.btn-default
         {:type "button"}
         [:span.ac.ac-search.form-control-feedback]]]]]]

    (when (:old data)
      [:div#warning.warning-version.alert.alert-default.fade.in
       [:button.close {:aria-hidden "true" :data-dismiss "alert" :type "button"} "×"]
       [:i.ac.ac-exclamation]
       (str " You are looking at an outdated " (:version data) " version of this document. Switch to the ")
       [:a {:href (:old-url data) :data-last-version "latest"}
        (:actual-version data)]
       " version to see the up to date information."])

    ;[:section#page-content.col-md-16.col-lg-18.col-lg-offset-6.col-md-offset-8
    ; [:div.row
    ;  [:div#article-content.col-lg-17
    ;   [:a#github-edit.btn.btn-default.btn-small.github-fork.pull-right.hidden-xs
    ;    {:href "https://github.com/AnyChart/docs.anychart.com"}
    ;    [:span [:i.ac.ac-net]] " Improve this Doc"] (-> data :page :content)]
    ;  [:div.col-lg-6.hidden-sm.hidden-xs.hidden-md.visible-lg
    ;   [:div#table-of-content-large]]]
    ; ]

    [:div#article-content.col-lg-9.col-lg-offset-9
     [:a#github-edit.btn.btn-default.btn-small.github-fork.pull-right.hidden-xs
      {:href "https://github.com/AnyChart/docs.anychart.com"}
      [:span [:i.ac.ac-net]] " Improve this Doc"] (-> data :page :content)]

    [:div.col-lg-3.hidden-sm.hidden-xs.hidden-md.visible-lg
     [:div.right-bar-side
      [:div#table-of-content-large]
      [:div.right-buttons
       [:a {:href (str "/" (:version data) "/download")}
        [:div.icon.get-offline]
        [:span "Get offline version"]]
       [:a
        [:div.icon.report2]
        [:span "Report issue"]]
       [:a {:href "https://github.com/AnyChart/docs.anychart.com"}
        [:div.icon.edit]
        [:span "Edit this page"]]
       ]]
     ]
    ]])



(defn page [data]
  (hiccup.page/html5
    {:lang "en"}
    (head data)
    (body data)
    [:div#shadow]
    (mobile-menu data)

    (main-content data)
    [:script {:type "text/javascript"}
     (str
       "window['version'] = '" (:version data) "';
        window['isUrlVersion'] = " (boolean (:is-url-version data)) ";")
     ]
    [:script {:type "text/javascript" :src "/main.min.js"}]

    )

  )
