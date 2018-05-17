(ns budget.core
  (:require
   [reagent.core :as reagent :refer [atom]]))


(enable-console-print!)


(defonce app-state
  (atom {:title "Budget!"}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App

(defn app
  []
  [:div {}
   [:h1.title (:title @app-state)]

   [:ul
    [:li#app "Hello from clojurescript"]]

   [:div.debug app-state]])


(defn mount!
  []
  (reagent/render [app] (js/document.querySelector "#cljs-target")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize

(mount!)
