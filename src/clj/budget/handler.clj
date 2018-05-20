(ns budget.handler
  (:require
   [compojure.route :as r]
   [compojure.core :refer [defroutes GET POST ANY]]
   [hiccup.page :refer [html5 include-css include-js]]
   [ring.middleware
    [defaults :refer [site-defaults wrap-defaults]]
    [keyword-params :refer [wrap-keyword-params]]
    [params :refer [wrap-params]]]

   [budget.db :as db]))

(defn fmt-entry
  [{:keys [name funds]}]
  (str name ":" funds))

(defn index
  []
  (html5
   [:head
    [:title "Budget"]]
   [:body

    [:ul (map
          #(let [x (select-keys % [:name :funds])] [:li (fmt-entry x)])
          (db/get-categories))]

    [:div#cljs-target]
    (apply include-js ["/js/compiled/budget.js"])
    (apply include-css ["/css/style.css"])]))

(defroutes all-routes

  (GET "/" [] (index))

  (r/resources "/")

  (r/not-found
   (html5 "not found")))

(def my-app
  (-> all-routes wrap-keyword-params wrap-params))
