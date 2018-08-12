(ns budget.handler
  (:require
   [compojure.route :as r]
   [compojure.core :refer [routes GET POST ANY]]


   [ring.util.response :refer [redirect]]
   [ring.middleware
    [defaults :refer [site-defaults wrap-defaults]]
    [keyword-params :refer [wrap-keyword-params]]
    [params :refer [wrap-params]]]

   ;; Logging
   [taoensso.timbre :as logging]
   [taoensso.timbre.appenders.core :as appenders]

   [budget.db :as db]
   [budget.util :as util]
   [budget.render :as render]
   [budget.report :as report]))


(logging/merge-config!
 {:appenders
  {:spit (appenders/spit-appender {:fname "data/budget.log"})}})

(defn- app-routes
  [config]
  (routes
   (GET "/" []
        (report/maybe-generate-and-reset config)
        (render/index config))
   (POST "/add-category" [cat-name funds]
         (db/add-category cat-name
                          (util/parse-int funds))
         (redirect "/"))
   (POST "/spend" [cat-id dec-amount]
         (db/update-funds (util/parse-int cat-id)
                          (util/parse-int dec-amount)
                          :decrement)
         (redirect "/"))
   (POST "/delete-category" [cat-id]
         (db/delete-category (util/parse-int cat-id))
         (redirect "/"))
   (POST "/delete-transaction" [tx-id]
         (db/delete-transaction (util/parse-int tx-id))
         (redirect "/"))
   (POST "/update-name" [cat-id cat-name]
         (db/update-name (util/parse-int cat-id)
                         cat-name)
         (redirect "/"))
   (POST "/update-monthly-limit" [cat-id limit]
         (db/update-monthly-limit (util/parse-int cat-id)
                                  (util/parse-int limit))
         (redirect "/"))

   (GET "/investment" []
        (render/investment config))

   (POST "/investment/stock-add-transaction" [stock-name stock-date
                                              stock-buy stock-shares
                                              stock-rate stock-total
                                              stock-currency]
         (logging/info stock-name
                       stock-date
                       stock-shares
                       stock-buy
                       stock-rate
                       stock-total
                       stock-currency)
         (db/stock-transaction-add
          {:name stock-name
           ;; TODO
           :shortname "TBI"
           :day (util/->localdate stock-date)
           :shares (util/parse-int stock-shares)
           :buy (= stock-buy "on")
           :rate (util/parse-int stock-rate)
           :total (util/parse-int stock-total)
           :currency stock-currency})
         (redirect "/investment"))

   (r/resources "/")
   (r/not-found render/not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
