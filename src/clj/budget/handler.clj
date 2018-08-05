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
   [taoensso.timbre :as timbre]
   [taoensso.timbre.appenders.core :as appenders]

   [budget.db :as db]
   [budget.util :as util]
   [budget.render :as render]))


(timbre/merge-config!
 {:appenders
  {:spit (appenders/spit-appender {:fname "data/budget.log"})}})


(defn- app-routes
  [config]
  (routes
   (GET "/" [] (render/index config))
   (POST "/add-category" [cat-name funds]
         (db/add-category cat-name
                          (util/parse-int funds))
         (redirect "/"))
   (POST "/increment" [cat-id inc-amount]
         (db/update-funds (util/parse-int cat-id)
                          (util/parse-int inc-amount)
                          :increment)
         (redirect "/"))
   (POST "/decrement" [cat-id dec-amount]
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
         (db/update-name (util/parse-int cat-id) cat-name)
         (redirect "/"))
   (r/resources "/")
   (r/not-found render/not-found)))


(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
