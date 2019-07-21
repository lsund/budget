(ns finances.handler
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :as route]
            [finances.db :as db]
            [finances.report :as report]
            [finances.util.core :as util]
            [finances.util.date :as util.date]
            [finances.views.budget :as views.budget]
            [finances.views.budget.transaction-group
             :as
             views.budget.transaction-group]
            [finances.views.calibrate-start-balances
             :as
             views.calibrate-start-balances]
            [finances.views.debts :as views.debts]
            [finances.views.delete-category :as views.delete-category]
            [finances.views.budget.manage-category :as views.budget.manage-category]
            [finances.views.funds :as views.funds]
            [finances.views.reports :as views.reports]
            [finances.views.stocks :as views.stocks]
            [hiccup.page :refer [html5]]
            [me.lsund.routes :refer [generate-routes]]
            [medley.core :refer [map-keys]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [redirect]]
            [taoensso.timbre :as logging]
            [taoensso.timbre.appenders.core :as appenders]))

(logging/merge-config!
 {:appenders
  {:spit (appenders/spit-appender {:fname "data/finances.log"})}})

(def not-found (html5 "not found"))

(defn add-transaction [db tx-type id tx-date tx-buy
                       tx-shares tx-rate tx-total tx-currency]
  (println [db tx-type id tx-date tx-buy
            tx-shares tx-rate tx-total tx-currency])
  (db/add-row db
              tx-type
              {(if (= tx-type :stocktransaction)
                 :stockid
                 :fundid) id
               :acc "ISK"
               :day (util.date/->localdate tx-date)
               :shares (util/parse-int tx-shares)
               :buy (= tx-buy "on")
               :rate (util/parse-float tx-rate)
               :total (util/parse-float tx-total)
               :currency tx-currency}))

(defn budget-db-data [config db]
  {:total-finances (db/get-total-finances db)
   :total-remaining (db/get-total-remaining db)
   :total-spent (db/get-total-spent db)
   :categories (->> (db/all db :category)
                    (remove #(= (:label %) "Buffer"))
                    (filter (comp not :hidden))
                    (sort-by :label))
   :buffer (db/row db :category {:label "Buffer"})
   :category-ids->names (db/category-ids->names db)
   :monthly-transactions (db/get-unreported-transactions db config)})

(defn- app-routes [{:keys [db] :as config}]
  (routes
   (GET "/" [all]
        (views.budget/render {:config
                              config

                              :all?
                              (some? all)

                              :generate-report-div
                              (db/monthly-report-missing? db config)}
                             (budget-db-data config db)))
   (GET "/debts" [id]
        (views.debts/render config {:debts (db/all db :debt)}))
   (GET "/reports" [id]
        (views.reports/render config
                              {:report (when id
                                         (db/row db
                                                 :report
                                                 (util/parse-int id)))
                               :reports (db/all db :report)}))
   (GET "/stocks" []
        (views.stocks/render config
                             {:stocks
                              (db/all db :stock)
                              :stocktransactions
                              (db/get-stock-transactions db)}))
   (GET "/funds" []
        (views.funds/render config
                            {:funds
                             (db/all db :fund)
                             :fundtransactions
                             (db/get-fund-transactions db)}))
   (GET "/budget/manage-category" [id]
        (views.budget.manage-category/render config
                                             (assoc (budget-db-data config db)
                                                    :category
                                                    (db/row db
                                                            :category
                                                            (util/parse-int id)))))
   (GET "/budget/transaction-group" [id]
        (views.budget.transaction-group/render
         config
         {:transaction-group
          (db/get-unreported-transactions db
                                          config
                                          (util/parse-int id))}))
   (GET "/delete-category" [id]
        (views.delete-category/render id))
   (POST "/calibrate-start-balances" []
         (views.calibrate-start-balances/render
          config
          {:total-start-balance
           (db/get-total-finances db)
           :categories
           (->> (db/all db :category)
                (sort-by :label)
                (filter (comp not :hidden)))}))

   (POST "/add-category" [label funds]
         (db/add-category db
                          label
                          (util/parse-int funds))
         (redirect "/"))
   (POST "/add-debt" [label funds]
         (db/add db :debt {:label label :amount (util/parse-int funds)})
         (redirect "/debts"))
   (POST "/transfer/balance" [from to amount]
         (db/transfer-balance db
                              (util/parse-int from)
                              (util/parse-int to)
                              (util/parse-int amount))
         (redirect (str "/budget/transfer?id=" from)))
   (POST "/transfer/start-balance" [from to amount]
         (db/transfer-start-balance db
                                    (util/parse-int from)
                                    (util/parse-int to)
                                    (util/parse-int amount))
         (redirect (str "/budget/transfer?id=" from)))
   (POST "/transfer/both" [from to amount]
         (jdbc/with-db-transaction [t-db db]
           (db/transfer-balance t-db
                                (util/parse-int from)
                                (util/parse-int to)
                                (util/parse-int amount))
           (db/transfer-start-balance t-db
                                      (util/parse-int from)
                                      (util/parse-int to)
                                      (util/parse-int amount)))
         (redirect (str "/budget/transfer?id=" from)))
   (POST "/spend" [id dec-amount]
         (db/add-transaction db
                             (util/parse-int id)
                             (util/parse-int dec-amount)
                             :decrement)
         (redirect "/"))
   (POST "/delete-category" [id]
         (db/delete-category db (util/parse-int id))
         (redirect "/"))
   (POST "/hide-category" [id]
         (db/update-row db :category {:hidden true} (util/parse-int id))
         (redirect "/"))
   (POST "/transaction/update-note" [id note]
         (db/update-row db :transaction {:note note} (util/parse-int id))
         (redirect "/"))
   (POST "/delete-transaction" [tx-id]
         (db/remove-transaction db (util/parse-int tx-id))
         (redirect "/"))
   (POST "/update-label" [id label]
         (db/update-label db
                          (util/parse-int id)
                          label)
         (redirect "/"))
   (POST "/update-start-balance" [id start-balance]
         (db/update-start-balance db
                                  (util/parse-int id)
                                  (util/parse-int start-balance))
         (redirect "/"))

   (POST "/stocks/add-transaction" [stock-id stock-date
                                      stock-buy stock-shares
                                      stock-rate stock-total
                                      stock-currency]
         (add-transaction db
                          :stocktransaction
                          (util/parse-int stock-id)
                          stock-date
                          stock-buy
                          stock-shares
                          stock-rate
                          stock-total
                          stock-currency)
         (redirect "/stocks"))
   (POST "/stocks/delete-transaction" [stock-id]
         (logging/info stock-id)
         (db/delete db
                    :stocktransaction
                    (util/parse-int stock-id))
         (redirect "/stocks"))
   (POST "/funds/add-transaction" [fund-id fund-date
                                     fund-buy fund-shares
                                     fund-rate fund-total
                                     fund-currency]
         (add-transaction db
                          :fundtransaction
                          (util/parse-int fund-id)
                          fund-date
                          fund-buy
                          fund-shares
                          fund-rate
                          fund-total
                          fund-currency)
         (redirect "/funds"))
   (POST "/funds/delete-transaction" [fund-id]
         (db/delete db
                    :fundtransaction
                    (util/parse-int fund-id))
         (redirect "/funds"))
   (POST "/generate-report" req
         (report/generate config)
         (db/update-start-balances! db
                                    (map (fn [[x y]]
                                           {:id (util/parse-int x)
                                            :start-balance (util/parse-int y)})
                                         (map-keys #(-> %
                                                        str
                                                        (string/split #"-")
                                                        last)
                                                   (:params req))))
         (db/reset-month db)
         (redirect "/"))
   (POST "/merge-categories" [source-id dest-id]
         (db/merge-categories db
                              (util/parse-int source-id)
                              (util/parse-int dest-id))
         (redirect "/"))
   (route/resources "/")
   (route/not-found not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-json-params)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
