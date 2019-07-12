(ns finances.views.reports
  (:require
   [clojure.edn :as edn]
   [finances.html :as html]
   [finances.util.date :as util.date]
   [hiccup.page :refer [html5 include-css include-js]]
   [hiccup.form :refer [form-to]]
   [taoensso.timbre :as logging]))

(defn category-row
  [c]
  [:tr
   [:td (:label c)]
   [:td (:start_balance c)]
   [:td (:spent c)]])

(defn transaction-row
  [t]
  [:tr
   [:td (:label t)]
   [:td (:amount t)]
   [:td (:time t)]
   [:td (:note t)]])

(defn render
  [config {:keys [report reports]}]
  (html5
   [:head [:title "Finances"]]
   [:body.mui-container
    (html/navbar)
    (form-to [:get "reports"]
             [:select {:name "id"}
              (for [report reports]
                [:option {:value (:id report)} (:day report)])]
             [:button.mui-btn "View"])
    (if-let [report (edn/read-string (:file report))]
      [:div
       [:div
        [:p (str "Budget was " (get-in report [:summary :was]))]
        [:p (str "Total spent " (get-in report [:summary :spent]))]
        [:p (str "Total remaining " (get-in report [:summary :remaining]))]
        [:table
         [:thead
          [:tr
           [:th "Name"]
           [:th "Start Balance"]
           [:th "Spent"]]]
         [:tbody
          (for [c (:budget report)]
            (category-row c))
          [:tr
           [:td "Total"]
           [:td (apply + (map :start_balance (:budget report)))]
           [:td (apply + (map :spent (:budget report)))]]]]]
       [:br]
       [:br]
       [:div
        [:table
         [:thead
          [:tr
           [:th "Name"]
           [:th "Amount"]
           [:th "Date"]
           [:th "Note"]]]
         [:tbody
          (for [t (->> report
                       :transactions
                       (sort-by :time)
                       reverse)]
            (transaction-row t))]]]])
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))
