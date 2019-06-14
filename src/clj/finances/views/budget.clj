(ns finances.views.budget
  (:require [hiccup.form :refer [form-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [finances.util.date :as util.date]
            [finances.util.core :as util]
            [finances.html :as html]))

(defn category-row
  [category categories {:keys [invisible]}]
  [:tr {:class (when invisible "invisible")}

   [:td
    (form-to  [:post "/update-label"]
              [:input {:type :hidden :name "id" :value (:id category)}]
              [:input {:type :text :name "label" :value (:label category)}])]
   [:td (:start_balance category)]
   [:td (:balance category)]
   [:td
    (form-to [:post "/spend"]
             [:div
              [:input {:name "id" :type :hidden :value (:id category)}]
              [:input {:class "spend" :name "dec-amount" :type :number :placeholder "$"}]])]
   [:td (:spent category)]
   [:td (util/category-diff category)]
   [:td (form-to [:get "/budget/transfer"]
                 [:input {:name "id" :type :hidden :value (:id category)}]
                 [:button "T"])]
   [:td
    (form-to
     [:get "/delete-category"]
     [:input {:name "id" :type :hidden :value (:id category)}]
     [:button "X"])]])

(defn transaction-row
  [t]
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

(defn transaction-group-row [[id transactions]]
  [:tr
   [:td (:label (first transactions))]
   [:td (apply + (map :amount transactions))]
   [:td (form-to [:get "/budget/transaction-group"]
                 [:input {:type :hidden :name "id" :value id}]
                 [:button "D"])]])

(defn render [config db-data]
  (html5
   (html/navbar)
   [:head [:title "Finances"]]
   [:body.mui-container
    (when #_(:generate-report-div config) true
          (do
            [:div.generate-report-div
             (form-to [:post "/calibrate-start-balances"]
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
       [:th "Transfer"]
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
     [:h2 "Latest transactions"]
     [:table
      [:thead
       [:tr
        [:th "Name"]
        [:th "Amount"]
        [:th "Date"]
        [:th "Note"]
        [:th "Remove"]]]
      [:tbody
       (for [transaction (->> db-data
                              :monthly-transactions
                              (sort-by :ts)
                              reverse
                              (take 10))]
         (transaction-row transaction))]]
     [:h2 "Transaction summary"]
     [:table
      [:thead
       [:tr
        [:th "Name"]
        [:th "Total"]
        [:th "Details"]]]
      [:tbody
       (for [transaction-group (->> db-data
                                    :monthly-transactions
                                    (sort-by :ts)
                                    reverse
                                    (group-by :categoryid))]
         (transaction-group-row transaction-group))]]
     [:p (str "Total: " (apply + (map :amount (:monthly-transactions db-data))))]]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css ["/css/style.css" "//cdn.muicss.com/mui-0.9.41/css/mui.min.css"])]))
