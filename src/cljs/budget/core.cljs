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
  [:div "Hello from clojurescript"])


(defn mount!
  []
  (reagent/render [app] (js/document.querySelector "#cljs-target")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize

(mount!)
