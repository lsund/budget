(ns finances.views.assets
  (:require [hiccup.form :refer [form-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [finances.views.internal :refer [render layout]]
            [finances.html :as html]))

(defn transaction-row [t]
  [:tr
   [:td (:day t)]
   [:td (:tag t)]
   [:td (if (:buy t) "Buy" "Sell")]
   [:td (:shares t)]
   [:td (:rate t)]
   [:td (:total t)]
   [:td (:currency t)]
   [:td (form-to [:post "/assets/delete-transaction"]
                 [:input {:name "id" :type :hidden :value (:id t)}]
                 [:button "X"])]])

(defmethod render :assets [_ config {:keys [assets transactions] :as options}]
  (layout config
          options
          [:div.assets
           [:div
            [:h2 "Add new Transaction"]
            (form-to  [:post "/assets/add-transaction"]
                      [:select {:name "id"}
                       (for [asset assets]
                         [:option {:value (:id asset)} (:tag asset)])]
                      [:div
                       [:label "Date"]
                       [:input {:name "date" :type :date}]
                       [:label "Buy?"
                        [:input {:name "buy" :type :checkbox}]]
                       [:label "Shares"]
                       [:input {:class "number-input"
                                :name  "shares"
                                :type :number
                                :step "0.01"
                                :min  "0"}]
                       [:label "Rate"]
                       [:input {:class "number-input"
                                :name "rate"
                                :type :number
                                :step "0.01"
                                :min "0"}]
                       [:label "Total"]
                       [:input {:class "number-input"
                                :name "total"
                                :type :number
                                :step "0.01"
                                :min "0"}]
                       [:select {:name "currency"}
                        [:option {:value "SEK"} "SEK"]]]
                      [:button.mui-btn "Add Transaction"])]
           [:h2 "Transactions"]
           [:table
            [:thead
             [:tr
              [:th "Date"]
              [:th "Name"]
              [:th "Buy/Sell"]
              [:th "Shares"]
              [:th "Rate"]
              [:th "Total"]
              [:th "Currency"]
              [:th "Delete"]]]
            [:tbody
             (for [t (->> transactions (sort-by :day) reverse)]
               (transaction-row t))]]]))
