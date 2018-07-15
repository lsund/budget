(ns budget.db
  (:require
   [clojure.string :as s]
   [clojure.java.jdbc :as j]
   [com.stuartsierra.component :as c]

   [taoensso.timbre :as timbre]))


(defn stringify [k] (-> k name s/capitalize))


(def pg-db {:dbtype "postgresql"
            :dbname "budget"
            :user "postgres"})


(defn get-categories []
  (j/query pg-db ["select * from category"]))


(defn update-q
  [op]
  (case op
    :increment "update category set funds=funds+? where name=?"
    :decrement "update category set funds=funds-? where name=?"
    (throw (Exception. "update-q: Illegal operation"))))


(defn add-category
  [c x]
  (j/insert! pg-db :category {:name (stringify c) :funds x}))


(defn add-transaction
  [id x]
  (j/insert! pg-db
             :transaction
             {:categoryid id
              :amount x
              :ts (java.time.LocalDateTime/now)}))


(defn delete-category
  [c]
  (j/delete! pg-db :category ["name=?" (stringify c)]))


(defn update-funds
  [c id x op]
  (add-transaction id x)
  (j/execute! pg-db [(update-q op) x (stringify c)]))


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
