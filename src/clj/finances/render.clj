(ns finances.render
  (:require [taoensso.timbre :as logging]
            [hiccup.form :refer [form-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [finances.util.core :as util]
            [finances.util.date :as util.date]
            [finances.html :as html]))

(defn diff [{:keys [start_balance spent]}]
  (when (and start_balance spent)
    (-  start_balance spent)))

(defn category-row
  [c categories {:keys [invisible]}]
  [:tr {:class (when invisible "invisible")}

   [:td
    (form-to  [:post "/update-label"]
              [:input {:type :hidden :name "id" :value (:id c)}]
              [:input {:type :text :name "label" :value (:label c)}])]
   [:td
    (form-to [:post "/update-start-balance"]
             [:input {:type :hidden :name "id" :value (:id c)}]
             [:input {:class "start-balance"
                      :type :text
                      :name "start-balance"
                      :value (:start_balance c)}])]
   [:td (:balance c)]
   [:td
    (form-to [:post "/spend"]
             [:div
              [:input {:name "id" :type :hidden :value (:id c)}]
              [:input {:class "spend" :name "dec-amount" :type :number :placeholder "$"}]])]
   [:td (:spent c)]
   [:td (diff c)]
   [:td (form-to [:post "/transfer/start-balance"]
                 [:input {:name "from" :type :hidden :value (:id c)}]
                 [:input.short-input {:type :text :name "amount" :placeholder "$"}]
                 [:select {:name "to"}
                  (for [cat categories]
                    [:option {:value (:id cat)} (:label cat)])]
                 [:input.hidden {:type :submit}])]
   [:td (form-to [:post "/transfer/balance"]
                 [:input {:name "from" :type :hidden :value (:id c)}]
                 [:input.short-input {:type :text :name "amount" :placeholder "$"}]
                 [:select {:name "to"}
                  (for [cat categories]
                    [:option {:value (:id cat)} (:label cat)])]
                 [:input.hidden {:type :submit}])]
   [:td
    (form-to
     [:get "/delete-category"]
     [:input {:name "id" :type :hidden :value (:id c)}]
     [:button "X"])]])

(defn transaction-row [t]
  [:tr
   [:td (:label t)]
   [:td (:amount t)]
   [:td (util.date/fmt-date (:ts t))]
   [:td
    (form-to  [:post "/transaction/update-note"]
              [:input {:type :hidden :name "id" :value (:id t)}]
              [:input {:type :text :name "note" :value (:note t)}])]
   [:td (form-to [:post "/delete-transaction"]
                 [:input {:name "tx-id" :type :hidden :value (:id t)}]
                 [:button "X"])]])

(defn index [config db-data]
  (html5
   (html/navbar)
   [:head [:title "Finances"]]
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
       [:th "Start Balance"]
       [:th "Balance"]
       [:th "Spend"]
       [:th "Spent"]
       [:th "Diff"]
       [:th "Transfer Limit"]
       [:th "Transfer Balance"]
       [:th "Delete"]]]
     [:tbody
      (let [cs (conj (:categories db-data) (:buffer db-data))]
        (concat
         [(category-row (:buffer db-data) cs {})
          (category-row {} [] {:invisible true})]
         (for [c (:categories db-data)]
           (category-row c cs {}))))
      [:row
       [:td ""]
       [:td (:total-finances db-data)]
       [:td (:total-remaining db-data)]
       [:td ""]
       [:td (:total-spent db-data)]]]]
    [:div
     [:h3 "Add New Spend Category"]
     (form-to {:class "add-category"} [:post "/add-category"]
              [:input
               {:name "label" :type :text :placeholder "Category name"}]
              [:input {:name "funds" :type :number :value 0}]
              [:button.mui-btn "Add category"])]
    [:div
     [:h2 "This months transactions"]
     [:table
      [:thead
       [:tr
        [:th "Name"]
        [:th "Amount"]
        [:th "Date"]
        [:th "Note"]
        [:th "Remove"]]]
      [:tbody
       (for [t (->> db-data
                    :monthly-transactions
                    (sort-by :ts)
                    reverse)]
         (transaction-row t))]]
     [:p (str "Total: " (apply + (map :amount (:monthly-transactions db-data))))]]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css ["/css/style.css" "//cdn.muicss.com/mui-0.9.41/css/mui.min.css"])]))

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

(defn stocks
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
    (apply include-css ["/css/style.css" "//cdn.muicss.com/mui-0.9.41/css/mui.min.css"])]))

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

(defn funds
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

(defn delete-category? [id]
  (html5
   [:head [:title "Warning"]]
   [:body.mui-container
    [:p (str "Deleting the category will permanently delete the category "
             "and also delete all transactions for the category.")]
    [:p "Hiding the category will make it it will dissapear from the front page but can be restored."]
    (form-to [:post "/delete-category"]
             [:input {:name "id" :type :hidden :value id}]
             [:button "Continue"])
    (form-to [:post "/hide-category"]
             [:input {:name "id" :type :hidden :value id}]
             [:button "Hide"])
    [:div#cljs-target]]))

(def not-found (html5 "not found"))
