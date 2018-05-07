(ns budget.core
    (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "text is printed from src/budget/core.cljs. Go ahead and edit it and see reloading in action.")

(defonce app-state
  (atom {:title "Budget!"}))


(defn app
  []
  [:div {}
   [:h1.title (:title @app-state)]

   [:ul
    [:li "Test!"]]

   [:div.debug app-state]

   ,,,])

(defn mount!
  []
  (reagent/render [app] (js/document.querySelector "#cljs-target")))

(defn on-js-reload []

  ;; (swap! app-state assoc :title "Kuk")

  (mount!))

(mount!)
