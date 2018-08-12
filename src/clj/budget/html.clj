(ns budget.html
  (:require
   [hiccup.form :refer [form-to]]))

(defn navbar []
  [:div.mui-appbar
   [:table {:width "100%"}
    [:tr {:style "vertical-align:middle;"}
     [:td.mui--appbar-height
      (form-to [:get "/"]
               [:input {:type :submit :value "Index"}])]
     [:td.mui--appbar-height
      (form-to [:get "/investment"]
               [:input {:type :submit :value "Stocks"}])]]]])
