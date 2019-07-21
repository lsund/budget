(ns finances.views.budget.manage-category
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
     [:h2 (:label category)]
     (form-to [:post "/update-label"]
              [:input {:name "id" :type :hidden :value (:id category)}]
              [:label "Change Label: "]
              [:input {:name "label"
                       :type "text"
                       :value (:label category)}])
     (form-to [:post "/update-start-balance"]
              [:input {:name "id" :type :hidden :value (:id category)}]
              [:label "Change Start Balance: "]
              [:input {:name "start-balance"
                       :type "text"
                       :value (:start_balance category)}])]
    [:div
     [:div.left
      [:h2 (:label category)]
      (if (pos? (:balance category))
        [:div
         [:h3 "Current Balance"]
         (form-to [:post "/transfer/balance"]
                  [:input {:name "from" :type :hidden :value (:id category)}]
                  [:input.short-input {:type :text
                                       :name "amount"
                                       :placeholder "$"}]
                  [:select {:name "to"}
                   (for [cat categories]
                     [:option {:value (:id cat)} (:label cat)])]
                  [:input.hidden {:type :submit}])
         (form-to [:post "/transfer/balance"]
                  [:input {:name "from" :type :hidden :value (:id category)}]
                  [:input {:name "amount"
                           :type :submit
                           :value (:balance category)}]
                  [:select {:name "to"}
                   (for [cat categories]
                     [:option {:value (:id cat)} (:label cat)])]
                  [:input.hidden {:type :submit}])
         [:h3 (str "Start + Current Balance")]
         (form-to [:post "/transfer/both"]
                  [:input {:name "from" :type :hidden :value (:id category)}]
                  [:input.short-input {:type :text
                                       :name "amount"
                                       :placeholder "$"}]
                  [:select {:name "to"}
                   (for [cat categories]
                     [:option {:value (:id cat)} (:label cat)])]
                  [:input.hidden {:type :submit}])]
        [:p "Non-positive balance"])
      (if (pos? (:start_balance category))
        [:div
         [:h3 "Start Balance"]
         (form-to [:post "/transfer/start-balance"]
                  [:input {:name "from" :type :hidden :value (:id category)}]
                  [:input.short-input {:type :text
                                       :name "amount"
                                       :placeholder "$"}]
                  [:select {:name "to"}
                   (for [cat categories]
                     [:option {:value (:id cat)} (:label cat)])]
                  [:input.hidden {:type :submit}])]
        [:div "Non-positive start balance"])]
     [:div.right
      (views.budget/budget-table {:simple? true
                                  :highlight 4} db-data)]]
    (apply include-css (:styles config))]))
