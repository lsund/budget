(ns finances.views.debts
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [finances.views.internal :refer [render layout]]
            [hiccup.form :refer [form-to]]
            [finances.html :as html]))

(defmethod render :debts [_ config db-data]
  (layout config
          db-data
          [:div
           (form-to {} [:post "/add-debt"]
                    [:input
                     {:name "label" :type :text :placeholder "Debt label"}]
                    [:input {:name "funds" :type :number :value 0}]
                    [:button.mui-btn "Add Debt"])
           [:h3 "Debts"]
           [:ul
            (for [debt (:debts db-data)]
              [:li (str (:label debt) ":" (:amount debt))])]]))
