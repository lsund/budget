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

   ;; Logging
   [taoensso.timbre :as timbre]
   [taoensso.timbre.appenders.core :as appenders]

   [budget.db :as db]))

(timbre/merge-config!
 {:appenders
  {:spit (appenders/spit-appender {:fname "data/budget.log"})}})



(defn category-names
  []
  (let [cs (db/get-categories)
        ids (map :id cs)
        ns  (map :name cs)]
    (zipmap ids ns)))

(defn fmt-entry
  [{:keys [name funds]}]
  (format "%s %s" name funds))

(defn parse-int [s] (Integer. (re-find  #"\d+" s)))

(defn entry
  [e]
  (let [label (-> e (select-keys [:name :funds]) fmt-entry)]
    [:li.entry
     [:div.title
      (form-to {:class "mui-form--inline"} [:get "/update-name"]
               [:div.mui-textfield
                [:input {:name  "category-name"
                         :type  :text
                         :value (str (:name e))}]])]

     [:div.funds (str "Current funds: " (:funds e))]

     [:div.earn
      (form-to {:class "mui-form--inline"} [:get "/increment"]
               [:div.mui-textfield
                [:input {:name "category-id" :type :hidden :value (:id e)}]
                [:input {:name "category-name" :type :hidden :value (:name e)}]
                [:label "Earn "]
                [:input {:name "inc-amount" :type :number}]])]
     [:div.spend
      (form-to {:class "mui-form--inline"} [:get "/decrement"]
               [:div.mui-textfield
                [:input {:name "category-id" :type :hidden :value (:id e)}]
                [:input {:name "category-name" :type :hidden :value (:name e)}]
                [:label "Spend "]
                [:input {:name "dec-amount" :type :number}]])]
     (form-to
      [:get "/delete"]
      [:div.delete
       [:input {:name "category-name" :type :hidden :value (:name e)}]
       [:button.mui-btn.mui-btn--primary "Delete"]])]))


(defn transaction
  [t]
  (let [names (category-names)]
    [:tr
     [:td (names (:categoryid t))]
     [:td (:amount t)]
     [:td (:ts t)]]))


(defn index
  [config]
  (html5
   [:head
    [:title "Budget"]]
   [:body.mui-container

    [:h3 "Budget"]
    [:ul (map entry (sort-by :name (db/get-categories)))]

    [:h3 "Transactions"]
    [:table.mui-table
     [:thead
      [:tr [:th "Name"] [:th "Amount"] [:th "Time"]]]
     [:tbody
      (for [t (db/get-transactions)]
        (transaction t))]]

    [:h3 "Add new category"]
    (form-to {:class "add"} [:get "/add"]

             [:div.mui-textfield
              [:input
               {:name "category-name" :type :text :placeholder "Category name"}]]

             [:div
              [:label "Value "]
              [:input {:name "funds" :type :number}]
              [:button.mui-btn.mui-btn--primary "Add category"]])

    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(defn- app-routes
  [config]
  (routes

   (GET "/" [] (index config))

   (GET "/add" [category-name funds]
        (db/add-category category-name (parse-int funds))
        (redirect "/"))

   (GET "/increment" [category-name category-id inc-amount]
        (db/update-funds
         category-name
         (parse-int category-id)
         (parse-int inc-amount)
         :increment)
        (redirect "/"))

   (GET "/decrement" [category-name category-id dec-amount]
        (db/update-funds
         category-name
         (parse-int category-id)
         (parse-int dec-amount)
         :decrement)
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
