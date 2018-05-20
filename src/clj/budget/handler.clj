(ns budget.handler
  (:require
   [compojure.route :as r]
   [compojure.core :refer [defroutes GET POST ANY]]
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
     (form-to
      [:get "/increment"]
      [:label label]
      [:input {:name "category-name" :type :hidden :value (:name e)}]
      [:input {:name "inc-amount" :type :number}])]))


(defn index
  []
  (html5
   [:head
    [:title "Budget"]]
   [:body

    [:ul (map entry (db/get-categories))]

    (form-to [:get "/add"]
             [:label "Add category"]
             [:input {:name "category-name" :type :text}]
             [:input {:name "funds" :type :number}]
             [:input {:type :submit}])


    [:div#cljs-target]
    (apply include-js ["/js/compiled/budget.js"])
    (apply include-css ["/css/style.css"])]))

(defroutes all-routes

  (GET "/" [] (index))

  (GET "/add" [category-name funds]
       (db/add-category category-name (parse-int funds))
       (redirect "/"))

  (GET "/increment" [category-name inc-amount]
       (db/update-funds category-name (parse-int inc-amount) :increment)
       (redirect "/"))

  (r/resources "/")

  (r/not-found
   (html5 "not found")))

(def my-app
  (-> all-routes wrap-keyword-params wrap-params))
