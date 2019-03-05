(ns finances.handler
  (:require [finances.db :as db]
            [finances.render :as render]
            [finances.report :as report]
            [finances.util.core :as util]
            [finances.util.date :as util.date]
            [me.lsund.routes :refer [generate-routes]]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :as route]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [redirect]]
            [taoensso.timbre :as logging]
            [taoensso.timbre.appenders.core :as appenders]))

(logging/merge-config!
 {:appenders
  {:spit (appenders/spit-appender {:fname "data/finances.log"})}})

(defn add-transaction [db tx-type id tx-date tx-buy
                       tx-shares tx-rate tx-total tx-currency]
  (println [db tx-type id tx-date tx-buy
            tx-shares tx-rate tx-total tx-currency])
  (db/add-transaction db
                      tx-type
                      {(if (= tx-type :stocktransaction) :stockid :fundid) id
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
   :categories (sort-by :balance > (db/get-all db :category {:except {:name "Buffer"}}))
   :buffer (db/row db :category {:name "Buffer"})
   :category-ids->names (db/category-ids->names db)
   :monthly-transactions (db/get-monthly-transactions db config)})

(defn- app-routes
  [{:keys [db] :as config}]
  (routes
   (generate-routes
    "resources/edn/routes.edn"
    (get-route :root []
               (let [extra (when (db/monthly-report-missing? db config)
                             {:generate-report-div true})]
                 (render/index (merge config extra)
                               (budget-db-data config db))))
    (get-route :stocks []
               (render/stocks config {:stocks (db/get-all db :stock)
                                      :stocktransactions (db/get-stock-transactions db)}))
    (get-route :funds []
               (render/funds config {:funds (db/get-all db :fund)
                                     :fundtransactions (db/get-fund-transactions db)}))
    (get-route [:category :delete] [cat-id]
               (render/delete-category?  cat-id))
    (post-route :generate-report []
                (report/generate config)
                (db/reset-month db)
                (redirect "/"))
    (post-route [:category :add] [cat-name funds]
                (db/add-category db
                                 cat-name
                                 (util/parse-int funds))
                (redirect "/"))
    (post-route [:transfer :balance] [from to amount]
                (db/transfer-balance db
                                     (util/parse-int from)
                                     (util/parse-int to)
                                     (util/parse-int amount))
                (redirect "/"))
    (post-route [:transfer :start-balance] [from to amount]
                (db/transfer-start-balance db
                                           (util/parse-int from)
                                           (util/parse-int to)
                                           (util/parse-int amount))
                (redirect "/"))
    (post-route :spend [cat-id dec-amount]
                (db/add-transaction db
                                    (util/parse-int cat-id)
                                    (util/parse-int dec-amount)
                                    :decrement)
                (redirect "/"))
    (post-route [:category :delete] [cat-id]
                (db/delete-category db (util/parse-int cat-id))
                (redirect "/"))
    (post-route [:transaction :update :note] [id note]
                (db/update-row db :transaction {:note note} (util/parse-int id))
                (redirect "/"))
    (post-route [:transaction :delete] [tx-id]
                (db/remove-transaction db (util/parse-int tx-id))
                (redirect "/"))
    (post-route [:category :update :name] [cat-id cat-name]
                (db/update-name db
                                (util/parse-int cat-id)
                                cat-name)
                (redirect "/"))
    (post-route [:category :update :start-balance] [cat-id start-balance]
                (db/update-start-balance db
                                         (util/parse-int cat-id)
                                         (util/parse-int start-balance))
                (redirect "/"))

    (post-route [:stocks :add :transaction] [stock-id stock-date
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
    (post-route [:stocks :delete :transaction] [stock-id]
                (logging/info stock-id)
                (db/delete db
                           :stocktransaction
                           (util/parse-int stock-id))
                (redirect "/stocks"))
    (post-route [:funds :add :transaction] [fund-id fund-date
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
    (post-route [:funds :delete :transaction] [fund-id]
                (db/delete db
                           :fundtransaction
                           (util/parse-int fund-id))
                (redirect "/funds")))

   (route/resources "/")
   (route/not-found render/not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
