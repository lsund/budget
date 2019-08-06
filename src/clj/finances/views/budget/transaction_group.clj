(ns finances.views.budget.transaction-group
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.form :refer [form-to]]
            [finances.views.internal :refer [render layout]]
            [finances.util.date :as util.date]
            [finances.html :as html]))

(defn transaction-row [transaction]
  [:tr
   [:td (:label transaction)]
   [:td (:amount transaction)]
   [:td (util.date/fmt-date (:time transaction))]
   [:td
    (form-to  [:post "/transaction/update-note"]
              [:input {:type :hidden :name "id" :value (:id transaction)}]
              [:input {:type :text :name "note" :value (:note transaction)}])]
   [:td (form-to [:post "/delete-transaction"]
                 [:input {:name "tx-id" :type :hidden :value (:id transaction)}]
                 [:button "X"])]])

(defmethod render :transaction-group [_ config {:keys [transaction-group] :as db-data}]
  (layout config
          db-data
          [:div
           [:h3 (str "Details for " (:label (first transaction-group)))]
           [:table
            [:thead
             [:tr
              [:th "Name"]
              [:th "Amount"]
              [:th "Date"]
              [:th "Note"]
              [:th "Remove"]]]
            [:tbody
             (for [transaction transaction-group]
               (transaction-row transaction))]]]))
