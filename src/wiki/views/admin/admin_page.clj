(ns wiki.views.admin.admin-page
  (:require [wiki.views.common :as common]
            [garden.core :as garden]
            [hiccup.page :as hiccup-page]))


;; =====================================================================================================================
;; bootstrap 4
;; =====================================================================================================================
(defn css-old []
  (garden/css
    {:pretty-print? false}
    [:body {:padding "10px"}]
    [:.version-label {:width   "200px"
                      :display "inline-block"
                      :padding "4px 10px"}]
    [:select.custom-select {:width "200px"}]
    [:.main {:width "580px"}]
    [:select :.btn-secondary :.btn-danger :.btn-group {:margin-right "10px"}]))


(defn page-old [versions]
  (hiccup-page/html5
    {:lang "en"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name    "viewport"
             :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
     [:link {:crossorigin "anonymous"
             :integrity   "sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm"
             :href        "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css"
             :rel         "stylesheet"}]
     [:link {:crossorigin "anonymous"
             :integrity   "sha384-O8whS3fhG2OnA5Kas0Y9l3cfpmYjapjI0E4theH4iuMD+pLhbf6JI0jIMfYcK3yZ"
             :href        "https://use.fontawesome.com/releases/v5.1.1/css/all.css",
             :rel         "stylesheet"}]
     [:style (css-old)]]

    [:body
     [:script {:src "/admin/main.js"}]
     [:script {:src "https://code.jquery.com/jquery-3.2.1.min.js"}]
     [:script {:crossorigin "anonymous"
               :integrity   "sha384-ApNbgh9B+Y1QKtv3Rn7W3mgPxhU9K/ScQsAP7hUibX39j7fakFPskvXusvfa0b4Q"
               :src         "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js"}]
     [:script {:crossorigin "anonymous"
               :integrity   "sha384-JZR6Spejh4U02d8jOt6vLEHfe/JQGiRRSQQxSfFWpi1MquVdAyjUar5+76PVCmYl"
               :src         "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"}]

     [:h5 "Version management"]

     [:div.main
      [:div.alert.alert-primary {:role "alert"}
       "To start update versions, just push this button." [:br]
       "This action is used by GitHub webhook."
       [:div [:a.btn.btn-success {:role "button"
                                  :href "/_update_"
                                  :rel  "nofollow"} "Start updating versions"]]]

      [:p                                                   ;.alert.alert-primary {:role "alert"}
       [:p "Select a version and choose an action."]
       [:div.form-group

        [:a.btn.btn-secondary {:role  "button"
                               :href  "/_admin_"
                               :title "Refresh page"
                               :rel   "nofollow"} [:i.fas.fa-sync-alt]]

        [:select.custom-select {:id "versionSelect"}
         (for [version versions]
           [:option {:value (:key version)} (:key version)])]

        [:div.btn-group
         [:button.btn.btn-primary.dropdown-toggle
          {:id            "dropdownMenuButton"
           :type          "button"
           :aria-expanded "false"
           :aria-haspopup "true"
           :data-toggle   "dropdown"
           :title         "Rebuild version"}
          "Rebuild"]
         [:div.dropdown-menu {:aria-labelledby "dropdownMenuButton"}
          [:a.dropdown-item {:id    "rebuildCommit"
                             :href  "#"
                             :title "Rebuild according to commit message flags"
                             :rel   "nofollow"}
           "commit message flags"]
          [:a.dropdown-item {:id    "rebuildFast"
                             :href  "#"
                             :title "Rebuild without link checking"
                             :rel   "nofollow"}
           "fast"]
          [:a.dropdown-item {:id    "rebuildLinkChecker"
                             :href  "#"
                             :title "Rebuild with link checking"
                             :rel   "nofollow"}
           "with link checking"]]]


        [:button.btn.btn-danger {:id    "deleteButton"
                                 :type  "button"
                                 :title "Remove version"} "Remove"]

        [:button.btn.btn-link {:id "showReportLink" :type "button"} "Show report"]]

       [:p
        [:a.btn.btn-link {:role "button"
                          :href "/_redirects_"
                          :rel  "nofollow"}
         "Show redirects"]]
       ]]
     ;(for [version versions]
     ;  [:div
     ;   [:span.version-label
     ;    (:key version)]
     ;   [:button.btn.btn-primary.btn-sm {:type "button"}
     ;    "Delete version"]
     ;   ]
     ;  )

     ]))




;; =====================================================================================================================
;; bootstrap 3
;; =====================================================================================================================
(defn page [{:keys [commit] :as data} versions]
  (hiccup-page/html5
    {:lang "en"}
    (common/head data false)
    [:body
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

         [:div.admin-panel
          [:h3 "Version management"]

          [:div..update-versions-box.row
           [:div.col-sm-15 [:div.text "To start update versions, just push this button." [:br]
                            "This action is used by GitHub webhook."]]
           ;[:br.visible-mobile]
           [:div.col-sm-9 [:a#updateVersionsButton.btn.btn-default.btn-success {:role "button"
                                                                                :type "button"
                                                                                :href "/_update_"
                                                                                :rel  "nofollow"} "Start updating versions"]]]

          [:p "Select a version and choose an action."]
          [:form.form-inline

           [:a#refreshButton.btn.btn-secondary {:role  "button"
                                                :href  "/_admin_"
                                                :title "Refresh page"
                                                :rel   "nofollow"}
            [:i.glyphicon.glyphicon-refresh]
            ;[:i.fas.fa-sync-alt]
            ]

           [:select.form-control.custom-select {:id "versionSelect"}
            (for [version versions]
              [:option {:value (:key version)} (:key version)])]

           [:div.btn-group
            [:button.btn.btn-primary.dropdown-toggle
             {:id            "dropdownMenuButton"
              :type          "button"
              :aria-expanded "false"
              :aria-haspopup "true"
              :data-toggle   "dropdown"
              :title         "Rebuild version"}
             "Rebuild "
             [:span.caret]]
            [:ul.dropdown-menu
             [:li [:a {:id    "rebuildCommit"
                       :href  "#"
                       :title "Rebuild according to commit message flags"
                       :rel   "nofollow"}
                   "Commit message flags"]]
             [:li [:a {:id    "rebuildFast"
                       :href  "#"
                       :title "Rebuild without link checking"
                       :rel   "nofollow"}
                   "Fast"]]
             [:li [:a {:id    "rebuildLinkChecker"
                       :href  "#"
                       :title "Rebuild with link checking"
                       :rel   "nofollow"}
                   "With link checking"]]]]

           [:button.btn.btn-danger {:id    "deleteButton"
                                    :type  "button"
                                    :title "Remove version"} "Remove"]

           [:button.btn.btn-link {:id "showReportLink" :type "button"} "Show report"]]

          [:p.other-buttons-box
           [:a.btn.btn-link {:role "button"
                             :href "/_redirects_"
                             :rel  "nofollow"}
            "Show redirects"]
           [:a.btn.btn-link {:id   "githubLink"
                             :role "button"
                             :href "https://github.com/AnyChart/docs.anychart.com"
                             :rel  "nofollow"}
            "GitHub Documentation"]
           [:a.btn.btn-link {:role "button"
                             :href "https://github.com/AnyChart/docs-engine"
                             :rel  "nofollow"}
            "GitHub Docs Engine"]]
          ]
         ]
        ]]]

     [:script {:type "text/javascript"} "window['version'] = 'latest';window['isUrlVersion']=false;"]
     [:script {:id "main_script" :type "text/javascript" :src (str "/main.min.js?v=" commit) :async true}]
     [:script {:src "https://code.jquery.com/jquery-3.2.1.min.js"}]
     [:script {:src (str "/admin/main.js?v=" commit)}]
     ]))