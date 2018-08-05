(ns budget.db
  (:require [clojure.java.jdbc :as j]
            [com.stuartsierra.component :as c]
            [taoensso.timbre :as timbre]
            [budget.util :as u]))


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


(defn get-sum []
  (j/query pg-db ["select sum(funds) from category"]))


(defn get-categories []
  (j/query pg-db ["select * from category"]))


(defn get-transactions []
  (j/query pg-db ["select * from transaction"]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Modify

;; Add

(defn add-category
  [cat-name funds]
  (j/insert! pg-db :category {:name (u/stringify cat-name) :funds funds}))


(defn add-transaction
  [cat-id x]
  (j/insert! pg-db
             :transaction
             {:categoryid cat-id
              :amount x
              :ts (java.time.LocalDateTime/now)}))


;; Update

(defn update-funds-q
  [op]
  (case op
    :increment "update category set funds=funds+? where id=?"
    :decrement "update category set funds=funds-? where id=?"
    (throw (Exception. "update-funds-q: Illegal operation"))))


(defn update-funds
  [cat-id x op]
  (add-transaction cat-id (case op :increment x :decrement (- x)))
  (j/execute! pg-db [(update-funds-q op) x cat-id]))


(defn update-name
  [cat-id cat-name]
  (j/execute! pg-db ["update category set name=? where id=?" cat-name cat-id]))


;; Delete

(defn delete-category
  [cat-id]
  (j/delete! pg-db :category ["id=?" cat-id]))


(defn delete-transaction
  [tx-id]
  (j/delete! pg-db :transaction ["id=?" tx-id]))
