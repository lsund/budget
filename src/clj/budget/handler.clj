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
   [budget.render :as render]))


(logging/merge-config!
 {:appenders
  {:spit (appenders/spit-appender {:fname "data/budget.log"})}})

(defn maybe-generate-report-and-reset [config]
  (when (util/is-25th?)
    (let [output-file (format "%s/test.txt" (:report-output-dir config))
          cat-ids->names (db/category-ids->names)]
      (spit  output-file "BUDGET:\n")
      (doseq [c (db/get-categories)]
        (spit output-file
              (format "%s %s %s\n"
                      (:name c)
                      (:monthly_limit c)
                      (:spent c))
              :append true))
      (spit output-file
            (format "\nSUMMARY:\nBudget was: %s\nTotal Remaining: %s\nTotal Spent: %s\n"
                    (db/get-total-budget)
                    (db/get-total-remaining)
                    (db/get-total-spent))
            :append true)
      (spit output-file "\nTRANSACTIONS:\n" :append true)
      (doseq [t (db/get-monthly-transactions)]
        (spit output-file
              (format "%s %s %s\n"
                      (cat-ids->names (:categoryid t))
                      (:amount t)
                      (util/fmt-date (:ts t)))
              :append true)))
    (logging/info "Generated Report")
    (db/reset-spent)
    (logging/info "Reset spent")))

(defn- app-routes
  [config]
  (routes
   (GET "/" []
        (maybe-generate-report-and-reset config)
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
   (r/resources "/")
   (r/not-found render/not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
