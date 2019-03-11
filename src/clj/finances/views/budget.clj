(ns finances.views.budget
  (:require [hiccup.form :refer [form-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [finances.util.date :as util.date]
            [finances.html :as html]))

(defn diff [{:keys [start_balance spent]}]
  (when (and start_balance spent)
    (-  start_balance spent)))

(defn category-row
  [category categories {:keys [invisible]}]
  [:tr {:class (when invisible "invisible")}

   [:td
    (form-to  [:post "/update-label"]
              [:input {:type :hidden :name "id" :value (:id category)}]
              [:input {:type :text :name "label" :value (:label category)}])]
   [:td
    (form-to [:post "/update-start-balance"]
             [:input {:type :hidden :name "id" :value (:id category)}]
             [:input {:class "start-balance"
                      :type :text
                      :name "start-balance"
                      :value (:start_balance category)}])]
   [:td (:balance category)]
   [:td
    (form-to [:post "/spend"]
             [:div
              [:input {:name "id" :type :hidden :value (:id category)}]
              [:input {:class "spend" :name "dec-amount" :type :number :placeholder "$"}]])]
   [:td (:spent category)]
   [:td (diff category)]
   [:td (form-to [:get "/budget/transfer"]
                 [:input {:name "id" :type :hidden :value (:id category)}]
                 [:button "T"])]
   [:td
    (form-to
     [:get "/delete-category"]
     [:input {:name "id" :type :hidden :value (:id category)}]
     [:button "X"])]])

(defn transaction-row [[id transactions]]
  [:tr
   [:td (:label (first transactions))]
   [:td (apply + (map :amount transactions))]
   [:td (form-to [:get "/budget/transaction-group"]
                 [:input {:type :hidden :name "id" :value id}]
                 [:button "Details"])]])

(defn render [config db-data]
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
       (for [transaction-group (->> db-data
                                    :monthly-transactions
                                    (sort-by :ts)
                                    reverse
                                    (group-by :categoryid))]
         (transaction-row transaction-group))]]
     [:p (str "Total: " (apply + (map :amount (:monthly-transactions db-data))))]]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css ["/css/style.css" "//cdn.muicss.com/mui-0.9.41/css/mui.min.css"])]))
