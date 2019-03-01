(ns budget.handler
  (:require [budget.db :as db]
            [budget.render :as render]
            [budget.report :as report]
            [budget.util.core :as util]
            [budget.util.date :as util.date]
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
  {:spit (appenders/spit-appender {:fname "data/budget.log"})}})

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

(defn select-keys-with-nil [m ks]
  "The result of [[(select-keys m ks)]] but if an element is present in ks but not in m,
   add it with value nil. Order is not necessarily retained.

   Example:
   (select-keys-with-nil m [:a :b :d])
    => {:b 2, :d nil, :a 1}"
  (let [present-keys (select-keys m ks)
        missing-keys (set/difference (set ks) (set (keys m)))]
    (merge (apply hash-map (interleave missing-keys (cycle [nil])))
           present-keys)))

(defmacro generate-routes [xs#]
  (let [route-spec# (edn/read-string (slurp "resources/edn/routes.edn"))]
    `(routes
      ~@(for [[method path args & body] xs#]
          (case method
            get-route `(GET ~(if (keyword path)
                               (get (:get route-spec#) path)
                               (get-in (:get route-spec#) path))
                            request-map#
                            (do
                              ((fn [{:keys ~args}] ~@body)
                                     (select-keys-with-nil (:params request-map#)
                                                           ~(mapv keyword args)))))
            post-route `(POST ~(if (keyword path)
                                 (get (:post route-spec#) path)
                                 (get-in (:post route-spec#) path))
                              request-map#
                              ((fn [{:keys ~args}] ~@body)
                                     (select-keys-with-nil (:params request-map#)
                                                           ~(mapv keyword args)))))))))

(defn- app-routes
  [{:keys [db] :as config}]
  (routes
   (generate-routes
    [(get-route :root []
                (let [extra (when (db/monthly-report-missing? db config)
                              {:generate-report-div true})]
                  (render/index (merge config extra)
                                {:total-budget (db/get-total-budget db)
                                 :total-remaining (db/get-total-remaining db)
                                 :total-spent (db/get-total-spent db)
                                 :categories (sort-by :funds > (db/get-all db :category))
                                 :category-ids->names (db/category-ids->names db)
                                 :monthly-transactions (db/get-monthly-transactions db config)})))
     (get-route :stocks []
                (render/stocks config {:stocks (db/get-all db :stock)
                                       :stocktransactions (db/get-all db :stocktransaction)}))
     (get-route :funds []
                (render/funds config {:funds (db/get-all db :fund)
                                      :fundtransactions (db/get-all db :fundtransaction)}))
     (post-route :generate-report []
                 (report/generate config)
                 (db/reset-month db)
                 (redirect "/"))
     (post-route [:category :add] [cat-name funds]
                 (db/add-category db
                                  cat-name
                                  (util/parse-int funds))
                 (redirect "/"))
     (post-route :transfer [from to amount]
                 (db/transfer-funds db
                                    (util/parse-int from)
                                    (util/parse-int to)
                                    (util/parse-int amount))
                 (redirect "/"))
     (post-route :spend [cat-id dec-amount]
                 (db/update-funds db
                                  (util/parse-int cat-id)
                                  (util/parse-int dec-amount)
                                  :decrement)
                 (redirect "/"))
     (post-route [:category :delete] [cat-id]
                 (db/delete db
                            :category
                            (util/parse-int cat-id))
                 (redirect "/"))
     (post-route [:transaction :delete] [tx-id]
                 (db/delete db
                            :transaction
                            (util/parse-int tx-id))
                 (redirect "/"))
     (post-route [:category :update :name] [cat-id cat-name]
                 (db/update-name db
                                 (util/parse-int cat-id)
                                 cat-name)
                 (redirect "/"))
     (post-route [:category :update :monthly-limit] [cat-id limit]
                 (db/update-monthly-limit db
                                          (util/parse-int cat-id)
                                          (util/parse-int limit))
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
                 (redirect "/funds"))])

   (route/resources "/")
   (route/not-found render/not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
