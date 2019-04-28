(ns finances.views.debts
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.form :refer [form-to]]
            [finances.html :as html]))

(defn render [config db-data]
  (html5
   (html/navbar)
   [:head [:title "Finances"]]
   [:body.mui-container
    [:p "Debts:"]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css ["/css/style.css" "//cdn.muicss.com/mui-0.9.41/css/mui.min.css"])]))
