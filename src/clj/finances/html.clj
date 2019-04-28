(ns finances.html
  (:require
   [hiccup.form :refer [form-to]]))

(defn navbar []
  [:div.mui-appbar
   [:table {:width "100%"}
    [:tr {:style "vertical-align:middle;"}
     [:td.mui--appbar-height
      (form-to [:get "/"]
               [:input {:type :submit :value "Budget"}])]
     [:td.mui--appbar-height
      (form-to [:get "/debts"]
               [:input {:type :submit :value "Debts"}])]
     [:td.mui--appbar-height
      (form-to [:get "/stocks"]
               [:input {:type :submit :value "Stocks"}])]
     [:td.mui--appbar-height
      (form-to [:get "/funds"]
               [:input {:type :submit :value "Funds"}])]
     [:td.mui--appbar-height
      (form-to [:get "/reports"]
               [:input {:type :submit :value "Budget reports"}])]]]])
