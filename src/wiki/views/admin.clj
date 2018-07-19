(ns wiki.views.admin
  (:require [garden.core :as garden]))

(defn css []
  (garden/css
    {:pretty-print? false}
    [:body {:padding "10px"}]
    [:.version-label {:width   "200px"
                      :display "inline-block"
                      :padding "4px 10px"}]
    [:select.custom-select {:width "200px"}]
    [:.main {:width "600px"}]
    [:.btn-danger :.btn-primary {:margin-left "10px"}]))


(defn page [versions]
  (hiccup.page/html5
    {:lang "en"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name    "viewport"
             :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]

     [:link {:crossorigin "anonymous"
             :integrity   "sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm"
             :href        "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css"
             :rel         "stylesheet"}]
     [:style (css)]
     ]

    [:body
     [:script {:src "/admin/main.js"}]
     [:script {
               ;:crossorigin "anonymous"
               ;:integrity   "sha384-KJ3o2DKtIkvYIK3UENzmM7KCkRr/rE9/Qpg6aAZGJwFDMVNA/GpGFF93hXpG5KkN"
               :src "https://code.jquery.com/jquery-3.2.1.min.js"}]
     [:script {:crossorigin "anonymous"
               :integrity   "sha384-ApNbgh9B+Y1QKtv3Rn7W3mgPxhU9K/ScQsAP7hUibX39j7fakFPskvXusvfa0b4Q"
               :src         "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js"}]
     [:script {:crossorigin "anonymous"
               :integrity   "sha384-JZR6Spejh4U02d8jOt6vLEHfe/JQGiRRSQQxSfFWpi1MquVdAyjUar5+76PVCmYl"
               :src         "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"}]

     [:h5 "Version management"]

     [:div.main
      [:div.alert.alert-primary {:role "alert"}
       "To start update versions, just push this button."
       [:div [:button.btn.btn-success {:id   "updateButton"
                                       :type "button"} "Start updating versions"]]]

      ;[:div [:a.btn.btn-success {:role "button"} "Start updating versions"]]

      [:div                                                 ;.alert.alert-primary {:role "alert"}
       [:p "Select a verson and choose an action.<br>If you want to regenerate a version, first you need to delete it.<br>Then you need to restart updating."]
       [:div.form-group
        [:select.custom-select {:id "versionSelect"}
         (for [version versions]
           [:option {:value (:key version)} (:key version)])]

        [:button.btn.btn-danger {:id "deleteButton" :type "button"}
         "Delete version"]

        [:button.btn.btn-primary {:id "checkLinksButton" :type "button"}
         "Check links"]

        [:button.btn.btn-link {:id "showReportLink" :type "button"}
         "Show report"]
        ]]]
     ;(for [version versions]
     ;  [:div
     ;   [:span.version-label
     ;    (:key version)]
     ;   [:button.btn.btn-primary.btn-sm {:type "button"}
     ;    "Delete version"]
     ;   ]
     ;  )

     ]))