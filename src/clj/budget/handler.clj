(ns budget.handler
  (:require
   [compojure.route :as r]
   [compojure.core :refer [routes GET POST ANY]]
   [hiccup.page :refer [html5 include-css include-js]]
   [hiccup.form :refer [form-to]]
   [ring.util.response :refer [redirect]]
   [ring.middleware
    [defaults :refer [site-defaults wrap-defaults]]
    [keyword-params :refer [wrap-keyword-params]]
    [params :refer [wrap-params]]]

   [budget.db :as db]))

(defn fmt-entry
  [{:keys [name funds]}]
  (str name ":" funds))

(defn parse-int [s] (Integer. (re-find  #"\d+" s)))

(defn entry
  [e]
  (let [label (-> e (select-keys [:name :funds]) fmt-entry)]
    [:li
     [:label label]
     (form-to
      [:get "/increment"]
      [:input {:name "category-name" :type :hidden :value (:name e)}]
      [:label "Earn "]
      [:input {:name "inc-amount" :type :number}])
     (form-to
      [:get "/decrement"]
      [:input {:name "category-name" :type :hidden :value (:name e)}]
      [:label "Spend "]
      [:input {:name "dec-amount" :type :number}])
     (form-to
      [:get "/delete"]
      [:input {:name "category-name" :type :hidden :value (:name e)}]
      [:input {:type :submit :value "Delete"}])]))

(defn index
  [config]
  (html5
   [:head
    [:title "Budget"]]
   [:body.mui-container

    [:ul (map entry (sort-by :name (db/get-categories)))]

    (form-to [:get "/add"]
             [:input {:name "category-name" :type :text}]
             [:input {:name "funds" :type :number}]
             [:input {:type :submit :value "Add Category"}]) [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(defn- app-routes
  [config]
  (routes

   (GET "/" [] (index config))

   (GET "/add" [category-name funds]
     (db/add-category category-name (parse-int funds))
     (redirect "/"))

   (GET "/increment" [category-name inc-amount]
     (db/update-funds category-name (parse-int inc-amount) :increment)
     (redirect "/"))

   (GET "/decrement" [category-name dec-amount]
     (db/update-funds category-name (parse-int dec-amount) :decrement)
     (redirect "/"))

   (GET "/delete" [category-name inc-amount]
     (db/delete-category category-name)
     (redirect "/"))

   (r/resources "/")

   (r/not-found
    (html5 "not found"))))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
