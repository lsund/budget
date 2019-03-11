(ns finances.views.budget.transfer
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.form :refer [form-to]]
            [finances.html :as html]))

(defn render [config {:keys [category categories]}]
  (html5
   (html/navbar)
   [:head [:title "Finances"]]
   [:body.mui-container
    [:h3 (str "Transfer start balance from " (:label category))]
    (form-to [:post "/transfer/start-balance"]
             [:input {:name "from" :type :hidden :value (:id category)}]
             [:input.short-input {:type :text :name "amount" :placeholder "$"}]
             [:select {:name "to"}
              (for [cat categories]
                [:option {:value (:id cat)} (:label cat)])]
             [:input.hidden {:type :submit}])
    [:h3 (str "Transfer current balance from " (:label category))]
    (form-to [:post "/transfer/balance"]
             [:input {:name "from" :type :hidden :value (:id category)}]
             [:input.short-input {:type :text :name "amount" :placeholder "$"}]
             [:select {:name "to"}
              (for [cat categories]
                [:option {:value (:id cat)} (:label cat)])]
             [:input.hidden {:type :submit}])

    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css ["/css/style.css" "//cdn.muicss.com/mui-0.9.41/css/mui.min.css"])]))
