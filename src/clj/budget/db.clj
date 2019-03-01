(ns budget.db
  (:require [clojure.java.jdbc :as j]
            [com.stuartsierra.component :as c]
            [taoensso.timbre :as logging]
            [budget.util.core :as util]
            [budget.util.date :as util.date]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Config

(defn pg-db [config]
  {:dbtype "postgresql"
   :dbname (:name config)
   :user "postgres"})

(def pg-db-val (pg-db {:name "budget"}))

(defrecord Db [db db-config]
  c/Lifecycle

  (start [component]
    (println ";; [Db] Starting database")
    (assoc component :db (pg-db db-config)))

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

(defn- previous-month [current-month]
  (if (= current-month 1) 12 (dec current-month)))

(defn get-monthly-transactions [db {:keys [salary-day]}]
  (let [month (.getValue (util.date/budget-month salary-day))]
    (j/query db [(str "select * from transaction where extract(month from ts) = ?"
                      " and extract(day from ts) >= ?"
                      " union all"
                      " select * from transaction where extract(month from ts) = ?"
                      " and extract(day from ts) <= ?")
                 (previous-month month)
                 salary-day
                 month
                 salary-day])))

(defn row [db table id]
  (first (j/query db [(str "SELECT * FROM " (name table) " WHERE id=?") id])))

(defn get-all [db table]
  (j/query db [(str "select * from " (name table))]))

(defn category-ids->names
  [db]
  (let [cats (get-all db :category)
        ids (map :id cats)
        ns  (map :name cats)]
    (zipmap ids ns)))

(defn monthly-report-missing?
  ([db config]
   (monthly-report-missing? db config (.getValue (util.date/budget-month (:salary-day config)))))
  ([db config month]
   (-> (j/query db ["select id from report where extract(month from day) = ?"
                    (previous-month month)])
       empty?)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Modify

;; Add

(defn add-category
  [db cat-name funds]
  (j/insert! db
             :category
             {:name (util/stringify cat-name)
              :funds funds
              :monthly_limit funds
              :spent 0}))


(defn add-report
  [db file]
  (j/insert! db :report {:file file
                         :day (util.date/today)}))

;; Delete

(defn delete
  [db table tx-id]
  (j/delete! db table ["id=?" tx-id]))

;; Update

(defn- decrease-funds [db amount cat-id]
  (j/execute! db ["update category set funds=funds-? where id=?" amount cat-id])
  (j/execute! db ["update category set spent=spent+? where id =?" amount cat-id]))

(defn- increase-funds [db amount cat-id]
  (j/execute! db ["update category set funds=funds+? where id=?" amount cat-id])
  (j/execute! db ["update category set spent=spent-? where id =?" amount cat-id]))

(defn add-transaction [db cat-id amount op]
  (j/insert! db :transaction {:categoryid cat-id
                    :amount (case op :increment amount :decrement (- amount))
                    :ts (java.time.LocalDateTime/now)})
  (decrease-funds db amount cat-id))

(defn remove-transaction [db tx-id]
  (let [{:keys [categoryid amount]} (row db :transaction tx-id)]
    (increase-funds db (- amount) categoryid)
    (delete db :transaction tx-id)))


(defn update-name [db cat-id cat-name]
  (j/execute! db ["update category set name=? where id=?" cat-name cat-id]))

(defn update-monthly-limit [db cat-id limit]
  (j/execute! db ["update category set monthly_limit=? where id=?" limit cat-id]))

(defn reset-spent [db]
  (j/execute! db ["update category set spent=0"]))

(defn reinitialize-monthly-budget [db]
  (j/execute! db ["update category set funds=monthly_limit"]))

(defn reset-month [db]
  (reset-spent db)
  (reinitialize-monthly-budget db))

(defn transfer-funds [db from to amount]
  (j/with-db-transaction [t-db db]
    (j/execute! t-db ["update category set funds=funds-? where id=?" amount from])
    (j/execute! t-db ["update category set funds=funds+? where id=?" amount to])))
