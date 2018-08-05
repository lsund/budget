(ns budget.render
  (:require
   [budget.db :as db]
   [taoensso.timbre :as timbre]
   [hiccup.form :refer [form-to]]
   [hiccup.page :refer [html5 include-css include-js]]
   [budget.util :as util]))


(defn fmt-entry
  [{:keys [name funds]}]
  (format "%s %s" name funds))


(defn entry
  [e]
  (let [label (-> e (select-keys [:name :funds]) fmt-entry)]
    [:tr

     [:td
      (form-to  [:post "/update-name"]
                [:input {:type :hidden :name "cat-id" :value (:id e)}]
                [:input {:type :text :name "cat-name" :value (:name e)}])]
     [:td (:funds e)]
     [:td
      (form-to [:post "/increment"]
               [:div
                [:input {:name "cat-id" :type :hidden :value (:id e)}]
                [:input {:name "inc-amount" :type :number}]])]

     [:td
      (form-to [:post "/decrement"]
               [:div
                [:input {:name "cat-id" :type :hidden :value (:id e)}]
                [:input {:name "dec-amount" :type :number}]])]

     [:td
      (form-to
       [:post "/delete-category"]
       [:input {:name "cat-id" :type :hidden :value (:id e)}]
       [:button "X"])]

     ,,,]))


(defn category-names
  []
  (let [cs (db/get-categories)
        ids (map :id cs)
        ns  (map :name cs)]
    (zipmap ids ns)))


(defn transaction
  [t]
  (let [names (category-names)]
    [:tr
     [:td (names (:categoryid t))]
     [:td (:amount t)]
     [:td (:ts t)]
     [:td (form-to [:post "/delete-transaction"]
                   [:input {:name "tx-id" :type :hidden :value (:id t)}]
                   [:button "X"])]]))


(defn index
  [config]
  (html5
   [:head [:title "Budget"]]
   [:body.mui-container
    [:h1 (util/get-current-date-header (:start-day config))]
    [:table.mui-table
     [:thead
      [:tr [:th "Name"] [:th "Current Funds"] [:th "Earn"] [:th "Spend"] [:th "Delete"]]]
     [:tbody
      (for [e (sort-by :name (db/get-categories))]
        (entry e))]]
    [:h3 (str "Total: " (-> (db/get-sum) first :sum) " Avail: " (:avail config))]
    [:div
     [:h2 "Add new category"]
     (form-to {:class "add-category"} [:post "/add-category"]
              [:div.mui-textfield
               [:input
                {:name "cat-name" :type :text :placeholder "Category name"}]]
              [:div
               [:label "Value "]
               [:input {:name "funds" :type :number :value 0}]
               [:button.mui-btn.mui-btn--primary "Add category"]])]
    [:div
     [:h2 "Latest Transactions"]
     [:table.mui-table
      [:thead
       [:tr [:th "Name"] [:th "Amount"] [:th "Time"] [:th "Remove"]]]
      [:tbody
       (for [t (->> (db/get-transactions)
                    (sort-by :ts)
                    reverse
                    (take (:n-transactions config)))]
         (transaction t))]]]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))


(def not-found (html5 "not found"))
