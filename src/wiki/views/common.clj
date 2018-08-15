(ns wiki.views.common)

;; =====================================================================================================================
;; Google Tag Manager https://developers.google.com/tag-manager/quickstart
;; =====================================================================================================================
(def head-tag-manager "<!-- Google Tag Manager -->
<script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
})(window,document,'script','dataLayer','GTM-5B8NXZ');</script>
<!-- End Google Tag Manager -->")


(def body-tag-manager "<!-- Google Tag Manager (noscript) -->
<noscript><iframe src=\"https://www.googletagmanager.com/ns.html?id=GTM-5B8NXZ\" height=\"0\" width=\"0\" style=\"display:none;visibility:hidden\"></iframe></noscript>
<!-- End Google Tag Manager (noscript) -->")


;; =====================================================================================================================
;; Common hiccup
;; =====================================================================================================================
(defn anychart-icon []
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
    [:span.chart-col.red]]])


(defn anychart-label []
  [:span.brand-label
   [:a.brand {:title  "AnyChart Home"
              :target "_blank"
              :rel    "nofollow"
              :href   "https://www.anychart.com"} "AnyChart"]
   [:a.documentation.hidden-extra-mobile {:title "AnyChart Documentation"
                                          :href  "/"} " Documentation"]])


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