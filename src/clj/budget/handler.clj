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

(def shortname->name
  {"HEXA B" "Hexagon B"
   "LATO B" "Latour B"
   "SWOL B" "Swedol B"
   "INVE B" "Investor B"
   "ALIG" "Alimak Group"
   "AXFO" "AXFOOD"
   "ELUX B" "Electrolux B"
   "MSON B" "Midsona B"
   "BAHN B" "Bahnhof B"
   "KIND SDB" "Kindred Group"
   "OP" "Oscar Properties"
   "WIHL" "Wihlborgs Fastigheter"
   "BOL" "Boliden"
   "NOKIA SEK" "Nokia Oyj"
   "NCC B" "NCC B"
   "VOLV B" "Volvo B"
   "DVMT" "Dell Technologies"
   "TSLA" "Tesla Inc"
   "SPWR" "Sunpower Corp"
   "AMZN" "Amazon.com Inc"
   "NOVO B" "Novo Nordisk B"
   "FING B" "Fingerprint Cards B"
   "TOBII" "Tobii"
   "SOLT" "Soltech Energy Sweden"
   "CLAS B" "Clas Ohlson B"
   "MACK B" "Mackmyra Svensk Whiskey B"
   "ATT" "Attendo"
   "WALL B" "Wallenstam B"
   "ERIC B" "Ericsson B"
   "AVZ" "Avanza Zero"
   "SAS" "Splitan Aktiefond Stabil"
   "SAI" "Splitan Aktiefond Investmentbolag"
   "SGI" "Splitan Globalfond Investmentbolag"
   "SH" "Splitan Högräntefond"
   "SRS" "Splitan Räntefond Sverige"})

(defn make-transaction [tx-type tx-code tx-date tx-buy
                        tx-shares tx-rate tx-total tx-currency]
  (db/transaction-add tx-type
                      {:name (shortname->name tx-code)
                       :acc "ISK"
                       :shortname tx-code
                       :day (util/->localdate tx-date)
                       :shares (util/parse-int tx-shares)
                       :buy (= tx-buy "on")
                       :rate (util/parse-int tx-rate)
                       :total (util/parse-int tx-total)
                       :currency tx-currency}))

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

   (GET "/stocks" []
        (render/stocks config))

   (POST "/stocks/add-transaction" [stock-code stock-date
                                    stock-buy stock-shares
                                    stock-rate stock-total
                                    stock-currency]
         (make-transaction :stocktransaction
                           stock-code stock-date stock-buy stock-shares
                           stock-rate stock-total stock-currency)
         (redirect "/stocks"))

   (POST "/stocks/delete-transaction" [stock-id]
         (logging/info stock-id)
         (db/stock-transaction-delete (util/parse-int stock-id))
         (redirect "/stocks"))

   (GET "/funds" []
        (render/funds config))

   (POST "/funds/add-transaction" [fund-code fund-date
                                   fund-buy fund-shares
                                   fund-rate fund-total
                                   fund-currency]
         (make-transaction :fundtransaction
                           fund-code fund-date fund-buy fund-shares
                           fund-rate fund-total fund-currency)
         (redirect "/funds"))

   (POST "/funds/delete-transaction" [fund-id]
         (db/fund-transaction-delete (util/parse-int fund-id))
         (redirect "/funds"))

   (r/resources "/")
   (r/not-found render/not-found)))

(defn new-handler
  [config]
  (-> (app-routes config)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-defaults
       (-> site-defaults (assoc-in [:security :anti-forgery] false)))))
