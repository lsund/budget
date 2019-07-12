(ns finances.views.debts
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.form :refer [form-to]]
            [finances.html :as html]))

(defn render [config db-data]
  (html5
   (html/navbar)
   [:head [:title "Finances"]]
   [:body.mui-container
    (form-to {} [:post "/add-debt"]
             [:input
              {:name "label" :type :text :placeholder "Debt label"}]
             [:input {:name "funds" :type :number :value 0}]
             [:button.mui-btn "Add Debt"])
    [:h3 "Debts"]
    [:ul
     (for [debt (:debts db-data)]
       [:li (str (:label debt) ":" (:amount debt))])]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))
