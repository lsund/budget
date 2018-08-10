(ns budget.render
  (:require
   [budget.db :as db]
   [taoensso.timbre :as logging]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [budget.util :as util]))


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
  [config]
  (html5
   [:head [:title "Budget"]]
   [:body.mui-container
    [:h1 (util/get-current-date-header (:start-day config))]
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
      (for [c (sort-by :name (db/get-categories))]
        (category-row c))
      [:row
       [:td ""]
       [:td (db/get-total-budget)]
       [:td (db/get-total-remaining)]
       [:td ""]
       [:td (db/get-total-spent)]]]]
    [:div
     [:h2 "Add new category"]
     (form-to {:class "add-category"} [:post "/add-category"]
              [:div.mui-textfield
               [:input
                {:name "cat-name" :type :text :placeholder "Category name"}]]
              [:div
               [:label "Value "]
               [:input {:name "funds" :type :number :value 0}]
               [:button.mui-btn "Add category"]])]
    [:div
     [:h2 "This months transactions"]
     [:table
      [:thead
       [:tr [:th "Name"] [:th "Amount"] [:th "Date"] [:th "Remove"]]]
      [:tbody
       (let [cat-ids->names (db/category-ids->names)]
         (for [t (->> (db/get-monthly-transactions)
                      (sort-by :ts)
                      reverse)]
           (transaction-row t cat-ids->names)))]]]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))


(def not-found (html5 "not found"))
