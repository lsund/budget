(ns budget.render
  (:require [taoensso.timbre :as logging]
            [hiccup.form :refer [form-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [budget.util.core :as util]
            [budget.util.date :as util.date]
            [budget.html :as html]))

(defn category-row
  [c categories]
  [:tr

   [:td
    (form-to  [:post "/update-name"]
              [:input {:type :hidden :name "cat-id" :value (:id c)}]
              [:input {:type :text :name "cat-name" :value (:name c)}])]
   [:td
    (form-to [:post "/update-limit"]
             [:input {:type :hidden :name "cat-id" :value (:id c)}]
             [:input {:class "limit"
                      :type :text
                      :name "limit"
                      :value (:limit c)}])]
   [:td (form-to [:post "/transfer/limit"]
                 [:input {:name "from" :type :hidden :value (:id c)}]
                 [:input.short-input {:type :text :name "amount" :placeholder "$"}]
                 [:select {:name "to"}
                  (for [cat categories]
                    [:option {:value (:id cat)} (:name cat)])]
                 [:input.hidden {:type :submit}])]
   [:td (:balance c)]
   [:td
    (form-to [:post "/spend"]
             [:div
              [:input {:name "cat-id" :type :hidden :value (:id c)}]
              [:input {:class "spend" :name "dec-amount" :type :number :placeholder "$"}]])]
   [:td (:spent c)]
   [:td (form-to [:post "/transfer"]
                 [:input {:name "from" :type :hidden :value (:id c)}]
                 [:input.short-input {:type :text :name "amount" :placeholder "$"}]
                 [:select {:name "to"}
                  (for [cat categories]
                    [:option {:value (:id cat)} (:name cat)])]
                 [:input.hidden {:type :submit}])]
   [:td
    (form-to
     [:post "/delete-category"]
     [:input {:name "cat-id" :type :hidden :value (:id c)}]
     [:button "X"])]])

(defn transaction-row
  [t cat-ids->names]
  [:tr
   [:td (cat-ids->names (:categoryid t))]
   [:td (:amount t)]
   [:td (util.date/fmt-date (:ts t))]
   [:td (:note t)]
   [:td (form-to [:post "/delete-transaction"]
                 [:input {:name "tx-id" :type :hidden :value (:id t)}]
                 [:button "X"])]])

(defn index [config db-data]
  (html5
   (html/navbar)
   [:head [:title "Budget"]]
   [:body.mui-container
    (when (:generate-report-div config)
      (do
        [:div.generate-report-div
         (form-to [:post "/generate-report"]
                  [:label "I don't see a report for last month. Generate one now?"]
                  [:button "Yes"])]))
    [:h1 (util.date/get-current-date-header (:salary-day config))]
    [:table
     [:thead
      [:tr
       [:th "Name"]
       [:th "Limit"]
       [:th "Transfer to"]
       [:th "Current Funds"]
       [:th "Spend"]
       [:th "Spent"]
       [:th "Transfer to"]
       [:th "Delete"]]]
     [:tbody
      (let [cs (:categories db-data)]
        (for [c cs]
          (category-row c cs)))
      [:row
       [:td ""]
       [:td (:total-budget db-data)]
       [:td (:total-remaining db-data)]
       [:td ""]
       [:td (:total-spent db-data)]]]]
    [:div
     [:h3 "Add New Spend Category"]
     (form-to {:class "add-category"} [:post "/add-category"]
              [:input
               {:name "cat-name" :type :text :placeholder "Category name"}]
              [:input {:name "funds" :type :number :value 0}]
              [:button.mui-btn "Add category"])]
    [:div
     [:h2 "This months transactions"]
     [:table
      [:thead
       [:tr [:th "Name"] [:th "Amount"] [:th "Date"] [:th "Note"] [:th "Remove"]]]
      [:tbody
       (for [t (->> db-data
                    :monthly-transactions
                    (sort-by :ts)
                    reverse)]
         (transaction-row t (:category-ids->names db-data)))]]
     [:p (str "Total: " (apply + (map :amount (:monthly-transactions db-data))))]]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css ["/css/style.css" "//cdn.muicss.com/mui-0.9.41/css/mui.min.css"])]))

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
  [config {:keys [stocks stocktransactions]}]
  (html5
   [:head [:title "Budget"]]
   [:body.mui-container
    (html/navbar)
    [:div.stocks
     [:div
      [:h2 "Add new Stock Transaction"]
      (form-to  [:post "/stocks/add-transaction"]
                [:select {:name "stock-id"}
                 (for [stock stocks]
                   [:option {:value (:id stock)} (:shortname stock)])]
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
    (apply include-css ["/css/style.css" "//cdn.muicss.com/mui-0.9.41/css/mui.min.css"])]))

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
  [config {:keys [funds fundtransactions]}]
  (html5
   [:head [:title "Budget"]]
   [:body.mui-container
    (html/navbar)
    [:div.funds
     [:div
      [:h2 "Add new Fund Transaction"]
      (form-to  [:post "/funds/add-transaction"]
                [:select {:name "fund-id"}
                 (for [fund funds]
                   [:option {:value (:id fund)} (:shortname fund)])]
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

(def not-found (html5 "not found"))
