(ns wiki.views.common)


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