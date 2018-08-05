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

   [budget.db :as db]
   [budget.util :as u]))

(timbre/merge-config!
 {:appenders
  {:spit (appenders/spit-appender {:fname "data/budget.log"})}})


(def avail 927)

(def n-transactions 20)


(defn cat-names
  []
  (let [cs (db/get-categories)
        ids (map :id cs)
        ns  (map :name cs)]
    (zipmap ids ns)))

(defn fmt-entry
  [{:keys [name funds]}]
  (format "%s %s" name funds))

(defn entry
  [e]
  (let [label (-> e (select-keys [:name :funds]) fmt-entry)]
    [:tr

     [:td
      (form-to  [:post "/update-name"]
                [:input {:type :hidden :name "cat-id" :value (:id e)}]
                [:input {:type :text :name "cat-name" :value (:name e)}])]
     [:td (:funds e)]
     [:td
      (form-to [:get "/increment"]
               [:div
                [:input {:name "cat-id" :type :hidden :value (:id e)}]
                [:input {:name "inc-amount" :type :number}]])]

     [:td
      (form-to [:get "/decrement"]
               [:div
                [:input {:name "cat-id" :type :hidden :value (:id e)}]
                [:input {:name "dec-amount" :type :number}]])]

     [:td
      (form-to
       [:get "/delete-category"]
       [:input {:name "cat-id" :type :hidden :value (:id e)}]
       [:button "X"])]

     ,,,]))


(defn transaction
  [t]
  (let [names (cat-names)]
    [:tr
     [:td (names (:categoryid t))]
     [:td (:amount t)]
     [:td (:ts t)]
     [:td (form-to [:post "/delete-transaction"]
                   [:input {:name "tx-id" :type :hidden :value (:id t)}]
                   [:button "X"])]]))


(defn index
  [config]
  (html5
   [:head [:title "Budget"]]
   [:body.mui-container
    [:h1 "Budget"]
    [:table.mui-table
     [:thead
      [:tr [:th "Name"] [:th "Current Funds"] [:th "Earn"] [:th "Spend"] [:th "Delete"]]]
     [:tbody
      (for [e (sort-by :name (db/get-categories))]
        (entry e))]]
    [:h3 (str "Total: " (-> (db/get-sum) first :sum) " Avail: " avail)]
    [:div
     [:h2 "Add new category"]
     (form-to {:class "add-category"} [:get "/add-category"]
              [:div.mui-textfield
               [:input
                {:name "cat-name" :type :text :placeholder "Category name"}]]
              [:div
               [:label "Value "]
               [:input {:name "funds" :type :number :value 0}]
               [:button.mui-btn.mui-btn--primary "Add category"]])]
    [:div
     [:h2 "Latest Transactions"]
     [:table.mui-table
      [:thead
       [:tr [:th "Name"] [:th "Amount"] [:th "Time"] [:th "Remove"]]]
      [:tbody
       (for [t (->> (db/get-transactions) (sort-by :ts) reverse (take n-transactions))]
         (transaction t))]]]
    [:div#cljs-target]
    (apply include-js (:javascripts config))
    (apply include-css (:styles config))]))

(defn- app-routes
  [config]
  (routes
   (GET "/" [] (index config))
   (GET "/add-category" [cat-name funds]
        (db/add-category cat-name (u/parse-int funds))
        (redirect "/"))
   (GET "/increment" [cat-id inc-amount]
        (db/update-funds
         (u/parse-int cat-id)
         (u/parse-int inc-amount)
         :increment)
        (redirect "/"))
   (GET "/decrement" [cat-id dec-amount]
        (db/update-funds
         (u/parse-int cat-id)
         (u/parse-int dec-amount)
         :decrement)
        (redirect "/"))
   (GET "/delete-category" [cat-id]
        (db/delete-category (u/parse-int cat-id))
        (redirect "/"))
   (POST "/delete-transaction" [tx-id]
        (db/delete-transaction (u/parse-int tx-id))
        (redirect "/"))
   (POST "update-name" [cat-id cat-name]
         (db/update-name (u/parse-int cat-id) cat-name))
   (r/resources "/")
   (r/not-found
    (html5 "not found"))))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
