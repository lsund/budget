(ns finances.views.budget.transfer
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.form :refer [form-to]]
            [finances.html :as html]))

(defn render [config {:keys [category categories]}]
  (html5
   (html/navbar)
   [:head [:title "Finances"]]
   [:body.mui-container
    [:h1 (str "Transfer start balance and current balance from "
              (:label category))]
    [:p "Start balance: " (:start_balance category)]
    [:p "Current balance: " (:balance category)]
    (form-to [:post "/transfer/both"]
             [:input {:name "from" :type :hidden :value (:id category)}]
             [:input.short-input {:type :text :name "amount" :placeholder "$"}]
             [:select {:name "to"}
              (for [cat categories]
                [:option {:value (:id cat)} (:label cat)])]
             [:input.hidden {:type :submit}])
    [:h3 (str "Transfer only start balance from " (:label category))]
    (form-to [:post "/transfer/start-balance"]
             [:input {:name "from" :type :hidden :value (:id category)}]
             [:input.short-input {:type :text :name "amount" :placeholder "$"}]
             [:select {:name "to"}
              (for [cat categories]
                [:option {:value (:id cat)} (:label cat)])]
             [:input.hidden {:type :submit}])
    [:h3 (str "Transfer only current balance from " (:label category))]
    (form-to [:post "/transfer/balance"]
             [:input {:name "from" :type :hidden :value (:id category)}]
             [:input.short-input {:type :text :name "amount" :placeholder "$"}]
             [:select {:name "to"}
              (for [cat categories]
                [:option {:value (:id cat)} (:label cat)])]
             [:input.hidden {:type :submit}])

    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))
