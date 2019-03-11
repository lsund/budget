(ns finances.views.funds
  (:require [hiccup.form :refer [form-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [finances.html :as html]))

(defn fund-transaction-row [t]
  [:tr
   [:td (:day t)]
   [:td (:tag t)]
   [:td (if (:buy t) "Buy" "Sell")]
   [:td (:shares t)]
   [:td (:rate t)]
   [:td (:total t)]
   [:td (:currency t)]
   [:td (form-to [:post "/funds/delete-transaction"]
                 [:input {:name "fund-id" :type :hidden :value (:id t)}]
                 [:button "X"])]])

(defn render
  [config {:keys [funds fundtransactions]}]
  (html5
   [:head [:title "Finances"]]
   [:body.mui-container
    (html/navbar)
    [:div.funds
     [:div
      [:h2 "Add new Fund Transaction"]
      (form-to  [:post "/funds/add-transaction"]
                [:select {:name "fund-id"}
                 (for [fund funds]
                   [:option {:value (:id fund)} (:tag fund)])]
                [:div
                 [:label "Date"]
                 [:input {:name "fund-date" :type :date}]
                 [:label "Buy?"
                  [:input {:name "fund-buy" :type :checkbox}]]
                 [:label "Shares"]
                 [:input {:class "number-input"
                          :name  "fund-shares"
                          :type :number
                          :step "0.01"
                          :min  "0"}]
                 [:label "Rate"]
                 [:input {:class "number-input"
                          :name "fund-rate"
                          :type :number
                          :step "0.01"
                          :min "0"}]
                 [:label "Total"]
                 [:input {:class "number-input"
                          :name "fund-total"
                          :type :number
                          :step "0.01"
                          :min "0"}]
                 [:select {:name "fund-currency"}
                  [:option {:value "SEK"} "SEK"]]]
                [:button.mui-btn "Add Transaction"])]
     [:h2 "Fund transactions"]
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
       (for [t (->> fundtransactions
                    (sort-by :day)
                    reverse)]
         (fund-transaction-row t))]]]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css ["/css/style.css" "//cdn.muicss.com/mui-0.9.41/css/mui.min.css"])]))
