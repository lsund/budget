(ns budget.core
    (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "text is printed from src/budget/core.cljs. Go ahead and edit it and see reloading in action.")

(defonce app-state
  (atom {:title "Budget!"}))

(println "1")

(defn app
  []
  [:div {}
   [:h1.title (:title @app-state)]

   [:ul
    [:li "Hello from clojurescript!"]]

   [:div.debug app-state]

   ,,,])

(println "2")

(defn mount!
  []
  (reagent/render [app] (js/document.querySelector "#cljs-target")))

(defn on-js-reload []

  ;; (swap! app-state assoc :title "Kuk")

  (mount!))

(mount!)
