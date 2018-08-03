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
  [c x]
  (j/insert! pg-db :category {:name (u/stringify c) :funds x}))


(defn add-transaction
  [id x]
  (j/insert! pg-db
             :transaction
             {:categoryid id
              :amount x
              :ts (java.time.LocalDateTime/now)}))


;; Update

(defn update-q
  [op]
  (case op
    :increment "update category set funds=funds+? where name=?"
    :decrement "update category set funds=funds-? where name=?"
    (throw (Exception. "update-q: Illegal operation"))))


(defn update-funds
  [c id x op]
  (add-transaction id (case op :increment x :decrement (- x)))
  (j/execute! pg-db [(update-q op) x (u/stringify c)]))


;; Delete

(defn delete-category
  [id]
  (j/delete! pg-db :category ["id=?" id]))


(defn delete-transaction
  [id]
  (j/delete! pg-db :transaction ["id=?" id]))
