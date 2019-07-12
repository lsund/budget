(ns finances.views.stocks
  (:require [hiccup.form :refer [form-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [finances.html :as html]))

(defn stock-transaction-row [t]
  [:tr
   [:td (:day t)]
   [:td (:tag t)]
   [:td (if (:buy t) "Buy" "Sell")]
   [:td (:shares t)]
   [:td (:rate t)]
   [:td (:total t)]
   [:td (:currency t)]
   [:td (form-to [:post "/stocks/delete-transaction"]
                 [:input {:name "stock-id" :type :hidden :value (:id t)}]
                 [:button "X"])]])

(defn render
  [config {:keys [stocks stocktransactions]}]
  (html5
   [:head [:title "Finances"]]
   [:body.mui-container
    (html/navbar)
    [:div.stocks
     [:div
      [:h2 "Add new Stock Transaction"]
      (form-to  [:post "/stocks/add-transaction"]
                [:select {:name "stock-id"}
                 (for [stock stocks]
                   [:option {:value (:id stock)} (:tag stock)])]
                [:div
                 [:label "Date"]
                 [:input {:name "stock-date" :type :date}]
                 [:label "Buy?"
                  [:input {:name "stock-buy" :type :checkbox}]]
                 [:label "Shares"]
                 [:input {:class "number-input"
                          :name  "stock-shares"
                          :type :number
                          :step "0.01"
                          :min  "0"}]
                 [:label "Rate"]
                 [:input {:class "number-input"
                          :name "stock-rate"
                          :type :number
                          :step "0.01"
                          :min "0"}]
                 [:label "Total"]
                 [:input {:class "number-input"
                          :name "stock-total"
                          :type :number
                          :step "0.01"
                          :min "0"}]
                 [:select {:name "stock-currency"}
                  [:option {:value "SEK"} "SEK"]]]
                [:button.mui-btn "Add Transaction"])]
     [:h2 "Stock transactions"]
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
       (for [t (->> stocktransactions
                    (sort-by :day)
                    reverse)]
         (stock-transaction-row t))]]]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))
