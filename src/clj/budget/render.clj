(ns budget.render
  (:require
   [budget.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [budget.util :as util]
   [budget.html :as html]))

(defn fmt-category-row
  [{:keys [name funds]}]
  (format "%s %s" name funds))

(defn category-row
  [c]
  (let [label (-> c (select-keys [:name :funds]) fmt-category-row)]
    [:tr

     [:td
      (form-to  [:post "/update-name"]
                [:input {:type :hidden :name "cat-id" :value (:id c)}]
                [:input {:type :text :name "cat-name" :value (:name c)}])]
     [:td
      (form-to {:class "mui-form--inline"}
               [:post "/update-monthly-limit"]
               [:input {:type :hidden :name "cat-id" :value (:id c)}]
               [:input {:class "limit"
                        :type :text
                        :name "limit"
                        :value (:monthly_limit c)}])]
     [:td (:funds c)]
     [:td
      (form-to [:post "/spend"]
               [:div
                [:input {:name "cat-id" :type :hidden :value (:id c)}]
                [:input {:class "spend" :name "dec-amount" :type :number}]])]
     [:td (:spent c)]

     [:td
      (form-to
       [:post "/delete-category"]
       [:input {:name "cat-id" :type :hidden :value (:id c)}]
       [:button "X"])]

     ,,,]))

(defn transaction-row
  [t cat-ids->names]
  [:tr
   [:td (cat-ids->names (:categoryid t))]
   [:td (:amount t)]
   [:td (util/fmt-date (:ts t))]
   [:td (form-to [:post "/delete-transaction"]
                 [:input {:name "tx-id" :type :hidden :value (:id t)}]
                 [:button "X"])]])

(defn index
  [{:keys [db] :as config}]
  (html5
   (html/navbar)
   [:head [:title "Budget"]]
   [:body.mui-container
    [:h1 (util/get-current-date-header (:salary-day config))]
    [:div
     [:h3 "Add New Spend Category"]
     (form-to {:class "add-category"} [:post "/add-category"]
              [:input
               {:name "cat-name" :type :text :placeholder "Category name"}]
              [:div [:input {:name "funds" :type :number :value 0}]]
              [:button.mui-btn "Add category"])]
    [:table
     [:thead
      [:tr
       [:th "Name"]
       [:th "Limit"]
       [:th "Current Funds"]
       [:th "Spend"]
       [:th "Spent"]
       [:th "Delete"]]]
     [:tbody
      (for [c (sort-by :name (db/get-all db :category))]
        (category-row c))
      [:row
       [:td ""]
       [:td (db/get-total-budget db)]
       [:td (db/get-total-remaining db)]
       [:td ""]
       [:td (db/get-total-spent db)]]]]
    [:div
     [:h2 "This months transactions"]
     [:table
      [:thead
       [:tr [:th "Name"] [:th "Amount"] [:th "Date"] [:th "Remove"]]]
      [:tbody
       (let [cat-ids->names (db/category-ids->names db)]
         (for [t (->> (db/get-monthly-transactions db)
                      (sort-by :ts)
                      reverse)]
           (transaction-row t cat-ids->names)))]]]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(defn stock-transaction-row [t]
  [:tr
   [:td (:day t)]
   [:td (:shortname t)]
   [:td (if (:buy t) "Buy" "Sell")]
   [:td (:shares t)]
   [:td (:rate t)]
   [:td (:total t)]
   [:td (:currency t)]
   [:td (form-to [:post "/stocks/delete-transaction"]
                 [:input {:name "stock-id" :type :hidden :value (:id t)}]
                 [:button "X"])]])

(defn stocks
  [{:keys [db] :as config}]
  (html5
   [:head [:title "Budget"]]
   [:body.mui-container

    (html/navbar)

    [:div.stocks
     [:div
      [:h2 "Add new Stock Transaction"]
      (form-to  [:post "/stocks/add-transaction"]
                [:div.mui-textfield
                 [:input
                  {:name "stock-code" :type :text :placeholder "Stock Code"}]]
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
       (for [t (->> (db/get-all db :stocktransaction)
                    (sort-by :day)
                    reverse)]
         (stock-transaction-row t))]]]

    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(defn fund-transaction-row [t]
  [:tr
   [:td (:day t)]
   [:td (:shortname t)]
   [:td (if (:buy t) "Buy" "Sell")]
   [:td (:shares t)]
   [:td (:rate t)]
   [:td (:total t)]
   [:td (:currency t)]
   [:td (form-to [:post "/funds/delete-transaction"]
                 [:input {:name "fund-id" :type :hidden :value (:id t)}]
                 [:button "X"])]])

(defn funds
  [{:keys [db] :as config}]
  (html5
   [:head [:title "Budget"]]
   [:body.mui-container

    (html/navbar)

    [:div.funds
     [:div
      [:h2 "Add new Fund Transaction"]
      (form-to  [:post "/funds/add-transaction"]
                [:div.mui-textfield
                 [:input
                  {:name "fund-code" :type :text :placeholder "Fund Code"}]]
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
       (for [t (->> (db/get-all db :fundtransaction)
                    (sort-by :day)
                    reverse)]
         (fund-transaction-row t))]]]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(def not-found (html5 "not found"))
