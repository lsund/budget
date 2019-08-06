(ns finances.views.delete-category
  (:require [taoensso.timbre :as logging]
            [hiccup.form :refer [form-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [finances.views.internal :refer [render layout]]
            [finances.util.core :as util]
            [finances.util.date :as util.date]
            [finances.html :as html]))

(defmethod render :delete-category [_ config {:keys [id] :as db-data}]
  (layout config db-data
          [:div
           [:p (str "Deleting the category will permanently delete the category "
                    "and also delete all transactions for the category.")]
           [:p "Hiding the category will make it it will dissapear from the front page but can be restored."]
           (form-to [:post "/delete-category"]
                    [:input {:name "id" :type :hidden :value id}]
                    [:button "Continue"])
           (form-to [:post "/hide-category"]
                    [:input {:name "id" :type :hidden :value id}]
                    [:button "Hide"])]))
