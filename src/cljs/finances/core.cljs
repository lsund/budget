(ns finances.core
  (:require
   [reagent.core :as reagent :refer [atom]]))


(enable-console-print!)


(defonce app-state
  (atom {:title "Finances!"}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App

(defn app
  []
  [:div ""])


(defn mount!
  []
  (reagent/render [app] (js/document.querySelector "#cljs-target")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize

(mount!)
