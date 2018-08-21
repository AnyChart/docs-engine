(ns wiki.views.common
  (:require [wiki.views.resources :as resources]
            [wiki.config.core :as c]
            [clojure.string :as string]))


(defn version-that-has-this-page-url [versions page-url]
  (first (filter #(.endsWith (:url %) page-url) versions)))


(defn canonical-page-url [versions page-url]
  (let [last-page-url-version (version-that-has-this-page-url versions page-url)]
    (if (= (:key last-page-url-version) (:key (first versions)))
      (str (c/domain) page-url)
      (str (c/domain) (:key last-page-url-version) (when page-url (str "/" page-url))))))


(defn styles [commit]
  (list
    [:link {:rel "stylesheet" :type "text/css" :href (str "/main.css?v=" commit)}]
    ;[:link {:rel "stylesheet" :type "text/css" :href (:anychart-css-url data)}]
    ; http://cdn.anychart.com/fonts/2.7.5/demo.html
    [:link {:rel "stylesheet" :type "text/css" :href "https://cdn.anychart.com/fonts/2.7.2/anychart.css"}]
    [:link {:rel "stylesheet" :type "text/css" :href "https://fonts.googleapis.com/css?family=Open+Sans:400,600"}]
    ;[:link {:rel "stylesheet" :type "text/css" :href "/lib/jquery-custom-content-scroller/jquery.mCustomScrollbar.min.css"}]
    [:link {:rel "apple-touch-icon" :sizes "57x57" :href "/icons/57.png"}]
    [:link {:rel "apple-touch-icon" :sizes "76x76" :href "/icons/76.png"}]
    [:link {:rel "apple-touch-icon" :sizes "120x120" :href "/icons/120.png"}]
    [:link {:rel "apple-touch-icon" :sizes "120x120" :href "/icons/152.png"}]
    [:link {:rel "apple-touch-icon" :sizes "152x152" :href "/icons/167.png"}]
    [:link {:rel "apple-touch-icon" :sizes "167x167" :href "/icons/180.png"}]))


(defn styles-head [commit]
  (list "<!--[if IE]>" (styles commit) "<![endif]-->"))


(defn styles-body [commit]
  (list "<!--[if !IE]> -->" (styles commit) "<!-- <![endif]-->"))


(defn head [{:keys [title-prefix
                    description
                    commit
                    image-url
                    versions
                    url] :as data}
            include-tag-mananger]
  [:head
   [:title title-prefix]

   (when include-tag-mananger resources/head-tag-manager)
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   (when description [:meta {:name "description" :content description}])

   [:meta {:property "og:title" :content title-prefix}]
   (when description [:meta {:property "og:description" :content description}])
   (when image-url [:meta {:property "og:image" :content (str "http://static.anychart.com/docs/images/" image-url ".png")}])
   [:meta {:property "og:type" :content "article"}]
   [:meta {:property "og:site_name" :content "AnyChart Documentation"}]
   [:meta {:property "article:publisher" :content "https://www.facebook.com/AnyCharts"}]
   [:meta {:property "fb:admins" :content "704106090"}]

   (when (seq (-> data :page :tags))
     [:meta {:name "keywords" :content (string/join ", " (-> data :page :tags))}])

   [:meta {:content "c5fb5d43a81ea360" :name "yandex-verification"}]

   (when (and versions url)
     [:link {:rel "canonical" :href (canonical-page-url versions url)}])

   [:link {:type "image/x-icon" :href "/i/anychart.ico" :rel "icon"}]
   (styles-head commit)
   "<!--[if lt IE 9]>"
   [:script {:scr "https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"}]
   [:script {:scr "https://oss.maxcdn.com/libs/respond.js/1.3.0/respond.min.js"}]
   "<![endif]-->"])



(defn anychart-brand []
  (list
    ;[:a.navbar-brand {:rel "nofollow" :href "https://www.anychart.com"}
    ; [:img {:alt "AnyChart" :src "/i/logo-empty.png"}]
    ; [:div.chart-row
    ;  [:span.chart-col.green]
    ;  [:span.chart-col.orange]
    ;  [:span.chart-col.red]]]
    ;[:a.brand.hidden-super-small " Documentation"]
    [:a.navbar-brand {:title  "AnyChart Home"
                      :target "_blank"
                      :rel    "nofollow"
                      :href   "https://www.anychart.com"}
     ;[:img {:alt "AnyChart" :src "/i/logo-empty.png"}]
     [:div.border-icon]
     [:div.chart-row
      [:span.chart-col.green]
      [:span.chart-col.orange]
      [:span.chart-col.red]]
     [:span.brand {:title  "AnyChart Home"
                   :target "_blank"
                   :rel    "nofollow"
                   :href   "https://www.anychart.com"} "AnyChart"]]
    [:span.brand-label
     [:a.documentation.hidden-extra-mobile {:title "AnyChart Documentation"
                                            :href  "/"} " Documentation"]]))


(defn anychart-help []
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
      "Contact support"]]]])