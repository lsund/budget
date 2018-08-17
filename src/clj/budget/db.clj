(ns budget.db
  (:require [clojure.java.jdbc :as j]
            [com.stuartsierra.component :as c]
            [taoensso.timbre :as timbre]
            [budget.util :as util]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Config


(defn make-db [config]
  {:dbtype "postgresql"
   :dbname (:name config)
   :user "postgres"})


(defrecord Db [db db-config]
  c/Lifecycle

  (start [component]
    (println ";; [Db] Starting database")
    (assoc component :db (make-db db-config)))

  (stop [component]
    (println ";; [Db] Stopping database")
    component))


(defn new-db
  [config]
  (map->Db {:db-config config}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query

(defn get-total-spent [db]
  (-> (j/query db ["select sum(spent) from category"]) first :sum))

(defn get-total-budget [db]
  (-> (j/query db ["select sum(monthly_limit) from category"]) first :sum))

(defn get-total-remaining [db]
  (-> (j/query db ["select sum(funds) from category"]) first :sum))

(defn get-monthly-transactions [db]
  (j/query db ["select * from transaction where ts >= current_date - 30"]))

(defn get-all [db table]
  (j/query db [(str "select * from " (name table))]))

(defn category-ids->names
  [db]
  (let [cats (get-all db :category)
        ids (map :id cats)
        ns  (map :name cats)]
    (zipmap ids ns)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Modify

;; Add

(defn add-category
  [db cat-name funds]
  (j/insert! db
             :category
             {:name (util/stringify cat-name)
              :funds funds
              :monthly_limit 0
              :spent 0}))


(defn add-transaction
  [db cat-id x]
  (j/insert! db
             :transaction
             {:categoryid cat-id
              :amount x
              :ts (java.time.LocalDateTime/now)}))

(defn transaction-add
  [db table tx]
  (j/insert! db table tx))

;; Update

(defn update-funds
  [db cat-id x op]
  (add-transaction cat-id (case op :increment x :decrement (- x)))
  (j/execute! db ["update category set funds=funds-? where id=?" x cat-id])
  (j/execute! db ["update category set spent=spent+? where id =?" x cat-id]))


(defn update-name
  [db cat-id cat-name]
  (j/execute! db ["update category set name=? where id=?" cat-name cat-id]))


(defn update-monthly-limit
  [db cat-id limit]
  (j/execute! db ["update category set monthly_limit=? where id=?" limit cat-id]))


(defn reset-spent [db]
  (j/execute! db ["update category set spent=0"]))


;; Delete

(defn delete
  [db table tx-id]
  (j/delete! db table ["id=?" tx-id]))
