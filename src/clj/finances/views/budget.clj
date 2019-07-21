(ns finances.views.budget
  (:require [hiccup.form :refer [form-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [finances.util.date :as util.date]
            [finances.util.core :as util]
            [finances.html :as html]))

(defn category-row
  [{:keys [id label start_balance balance spent] :as category}
   categories
   {:keys [invisible? simple? highlight]}]
  (let [highlight-fn #(if (= id highlight) [:b %] %)]
    (-> [:tr {:class (when invisible? "invisible")}]
        (concat [[:td (highlight-fn label)]
                 [:td (highlight-fn start_balance)]
                 [:td {:class (when (and balance (neg? balance))
                                "red")} (highlight-fn balance)]]
                (when-not simple?
                  [[:td spent]
                   [:td (util/category-diff category)]
                   [:td (form-to [:get "/budget/manage-category"]
                                 [:input {:name "id" :type :hidden :value id}]
                                 [:button "M"])]
                   [:td
                    (form-to
                     [:get "/delete-category"]
                     [:input {:name "id" :type :hidden :value id}]
                     [:button "X"])]]))
        vec)))

(defn budget-table [{:keys [simple? all?] :as options}
                    {:keys [total-spent total-remaining total-finances]
                     :as db-data}]
  [:table
   [:thead
    (-> [:tr]
        (concat [[:th "Name"]
                 [:th "Start Balance"]
                 [:th "Balance"]]
                (when-not simple?
                  [[:th "Spent"]
                   [:th "Diff"]
                   [:th "Manage"]
                   [:th "Delete"]]))
        vec)]
   [:tbody
    (let [cs (conj (:categories db-data) (:buffer db-data))]
      (concat
       [(category-row (:buffer db-data) cs options)
        (category-row {} [] (assoc options :invisible? true))]
       (for [c (:categories db-data)]
         (when (or all?
                   (pos? (:balance c))
                   (pos? (:spent c)))
           (category-row c cs options)))))
    (-> [:tr]
        (concat [[:td ""]
                 [:td total-finances]
                 [:td {:class (when (neg? total-remaining) "red")}
                  total-remaining]]
                (when-not simple?
                  [[:td  total-spent]
                   [:td ""]]))
        vec)]])

(defn transaction-row
  [t]
  [:tr
   [:td (:label t)]
   [:td (:amount t)]
   [:td (util.date/fmt-date (:time t))]
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

(defn render [{:keys [all? config generate-report-div]}
              {:keys [total-spent total-remaining total-finances]
               :as db-data}]
  (html5
   (html/navbar)
   [:head [:title "Finances"]]
   [:body.mui-container
    (when generate-report-div
      (do
        [:div.generate-report-div
         (form-to [:post "/calibrate-start-balances"]
                  [:label "I don't see a report for last month.
                           Generate one now?"]
                  [:button "Yes"])]))
    [:h2 "Spend"]
    (form-to [:post "/spend"]
             [:div
              [:select {:name "id"}
               (for [cat (:categories db-data)]
                 [:option {:value (:id cat)} (:label cat)])]
              [:input {:class "spend"
                       :name "dec-amount"
                       :type :number
                       :placeholder "$"}]])
    [:h2 "Budget: " (util.date/get-current-date-header (:salary-day config))]
    [:a {:href "/?all=true"} "Show all"]
    (budget-table {:simple? false :all? all?} db-data)
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
                              (sort-by :time)
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
                                    (sort-by :time)
                                    reverse
                                    (group-by :categoryid))]
         (transaction-group-row transaction-group))]]
     [:p (str "Total: " (apply + (map :amount (:monthly-transactions db-data))))]]
    [:div
     [:h3 "Add New Spend Category"]
     (form-to {:class "add-category"} [:post "/add-category"]
              [:input
               {:name "label" :type :text :placeholder "Category name"}]
              [:input {:name "funds" :type :number :value 0}]
              [:button.mui-btn "Add category"])]
    (apply include-css (:styles config))]))
