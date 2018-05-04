(ns budget.core
    (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "text is printed from src/budget/core.cljs. Go ahead and edit it and see reloading in action.")

(defonce app-state
  (atom {:title "test"}))


(defn app
  []
  [:div {}
   [:h1.title "hello"]

   [:div.debug app-state]

   ,,,])

(defn mount!
  []
  (reagent/render [app] (js/document.querySelector "#cljs-target")))

(defn on-js-reload []

  (println "hello")

  (swap! app-state assoc :title "Kuk")

  (println app-state)
  (mount!))

(mount!)
