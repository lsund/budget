(ns budget.db
  (:require [clojure.java.jdbc :as j]
            [com.stuartsierra.component :as c]
            [taoensso.timbre :as timbre]
            [budget.util :as util]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Config


(def pg-db {:dbtype "postgresql"
            :dbname "budget"
            :user "postgres"})


(defrecord Db [db]
  c/Lifecycle

  (start [component]
    (println ";; [Db] Starting database")
    component)

  (stop [component]
    (println ";; [Db] Stopping database")
    component))


(defn new-db
  []
  (map->Db {}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query

(defn get-total-spent []
  (-> (j/query pg-db ["select sum(spent) from category"]) first :sum))

(defn get-total-budget []
  (-> (j/query pg-db ["select sum(monthly_limit) from category"]) first :sum))

(defn get-total-remaining []
  (-> (j/query pg-db ["select sum(funds) from category"]) first :sum))

(defn get-monthly-transactions []
  (j/query pg-db ["select * from transaction where ts >= current_date - 30"]))

(defn get-categories []
  (j/query pg-db ["select * from category"]))

(defn get-stock-transactions []
  (j/query pg-db ["select * from stocktransaction"]))

(defn get-fund-transactions []
  (j/query pg-db ["select * from fundtransaction"]))

(defn category-ids->names
  []
  (let [cats (get-categories)
        ids (map :id cats)
        ns  (map :name cats)]
    (zipmap ids ns)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Modify

;; Add

(defn add-category
  [cat-name funds]
  (j/insert! pg-db :category {:name (util/stringify cat-name) :funds funds}))


(defn add-transaction
  [cat-id x]
  (j/insert! pg-db
             :transaction
             {:categoryid cat-id
              :amount x
              :ts (java.time.LocalDateTime/now)}))


(defn stock-transaction-add
  [stock]
  (j/insert! pg-db :stocktransaction stock))

(defn fund-transaction-add
  [fund]
  (j/insert! pg-db :fundtransaction fund))

;; Update

(defn update-funds
  [cat-id x op]
  (add-transaction cat-id (case op :increment x :decrement (- x)))
  (j/execute! pg-db ["update category set funds=funds-? where id=?" x cat-id])
  (j/execute! pg-db ["update category set spent=spent+? where id =?" x cat-id]))


(defn update-name
  [cat-id cat-name]
  (j/execute! pg-db ["update category set name=? where id=?" cat-name cat-id]))


(defn update-monthly-limit
  [cat-id limit]
  (j/execute! pg-db ["update category set monthly_limit=? where id=?" limit cat-id]))


(defn reset-spent []
  (j/execute! pg-db ["update category set spent=0"]))


;; Delete

(defn delete-category
  [cat-id]
  (j/delete! pg-db :category ["id=?" cat-id]))


(defn delete-transaction
  [tx-id]
  (j/delete! pg-db :transaction ["id=?" tx-id]))
