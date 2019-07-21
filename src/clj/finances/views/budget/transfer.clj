(ns finances.views.budget.transfer
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.form :refer [form-to]]
            [finances.views.budget :as views.budget]
            [finances.html :as html]))

(defn render [config {:keys [category categories] :as db-data}]
  (html5
   (html/navbar)
   [:head [:title "Finances"]]
   [:body.mui-container
    [:div
     [:div.left
      [:h2 (:label category) " (from)"]
      [:p "Start balance: " (:start_balance category)]
      [:p "Current balance: " (:balance category)]
      [:h3 "Current Balance"]
      (form-to [:post "/transfer/balance"]
               [:input {:name "from" :type :hidden :value (:id category)}]
               [:input.short-input {:type :text :name "amount" :placeholder "$"}]
               [:select {:name "to"}
                (for [cat categories]
                  [:option {:value (:id cat)} (:label cat)])]
               [:input.hidden {:type :submit}])
      [:h3 "Start Balance"]
      (form-to [:post "/transfer/start-balance"]
               [:input {:name "from" :type :hidden :value (:id category)}]
               [:input.short-input {:type :text :name "amount" :placeholder "$"}]
               [:select {:name "to"}
                (for [cat categories]
                  [:option {:value (:id cat)} (:label cat)])]
               [:input.hidden {:type :submit}])
      [:h3 (str "Start + Current Balance")]
      (form-to [:post "/transfer/both"]
               [:input {:name "from" :type :hidden :value (:id category)}]
               [:input.short-input {:type :text :name "amount" :placeholder "$"}]
               [:select {:name "to"}
                (for [cat categories]
                  [:option {:value (:id cat)} (:label cat)])]
               [:input.hidden {:type :submit}])]
     [:div.right
      (views.budget/budget-table {:simple? true} db-data)]]
    (apply include-css (:styles config))]))
