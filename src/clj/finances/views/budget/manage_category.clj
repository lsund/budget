(ns finances.views.budget.manage-category
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.form :refer [form-to]]
            [finances.views.budget :as views.budget]
            [finances.views.internal :refer [render layout]]
            [finances.html :as html]))

(defmethod render :manage-category [_
                                    config
                                    {:keys [category categories] :as db-data}]
  (layout config db-data
          [:div
           [:div.left
            [:h2 (:label category)]
            [:h3 "Set Label"]
            (form-to [:post "/update-label"]
                     [:input {:name "id" :type :hidden :value (:id category)}]

                     [:input {:name "label"
                              :type "text"
                              :value (:label category)}])
            [:h3 "Set Start Balance"]
            (form-to [:post "/update-start-balance"]
                     [:input {:name "id" :type :hidden :value (:id category)}]
                     [:input {:name "start-balance"
                              :type "text"
                              :value (:start_balance category)}])
            (if (pos? (:balance category))
              [:div
               [:h3 "Transfer Current Balance"]
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
               [:h3 (str "Transfer Start + Current Balance")]
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
               [:h3 "Transfer Start Balance"]
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
                                        :highlight (:id category)} db-data)]]))
