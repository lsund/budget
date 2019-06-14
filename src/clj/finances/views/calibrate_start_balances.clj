(ns finances.views.calibrate-start-balances
  (:require [taoensso.timbre :as logging]
            [hiccup.form :refer [form-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [finances.util.core :as util]
            [finances.util.date :as util.date]
            [finances.html :as html]))

(defn category-row
  [category categories {:keys [invisible]}]
  [:tr
   [:td (:label category)]
   [:td
    [:input {:class "start-balance"
             :type :text
             :name (str "start-balance-" (:id category))
             :value (:start_balance category)}]]
   [:td (:start_balance category)]
   [:td (util/category-diff category)]])

(defn render [config db-data]
  (html5
   [:head [:title "Warning"]]
   [:body.mui-container

    [:p "Use this form to optionally calibrate the start balances of the different categories"]
    (form-to [:post "/generate-report"]
             [:table
              [:thead
               [:tr
                [:th "Name"]
                [:th "Start Balance"]
                [:th "Last month set"]
                [:th "Last month diff"]]]
              [:tbody
               (form-to [:post "/generate-report"] )
               (let [cs (conj (:categories db-data) (:buffer db-data))]
                 (for [c (:categories db-data)]
                   (category-row c cs {})))
               [:row
                [:td "hello"]
                [:td (:total-start-balance db-data)]]]]
             [:button "Generate report"])

    [:div#cljs-target]]))
