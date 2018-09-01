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
  (j/query db [(str "select * from transaction where extract(month from ts) = ?"
                    " and extract(day from ts) >= ?"
                    " union all"
                    " select * from transaction where extract(month from ts) = ?"
                    " and extract(day from ts) <= ?") 8 25 9 25])
  )

(defn get-all [db table]
  (j/query db [(str "select * from " (name table))]))

(defn category-ids->names
  [db]
  (let [cats (get-all db :category)
        ids (map :id cats)
        ns  (map :name cats)]
    (zipmap ids ns)))

(defn monthly-report-missing?
  ([db salary-day]
   (monthly-report-missing? db salary-day (.getValue (util/budget-month salary-day))))
  ([db salary-day month]
   (-> (j/query db ["select id from report where extract(month from day) = ? - 1" month])
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
              :monthly_limit 0
              :spent 0}))


(defn add-transaction
  [db table tx]
  (j/insert! db table tx))


(defn add-report
  [db file]
  (j/insert! db :report {:file file
                         :day (util/today)}))

;; Update

(defn update-funds
  [db cat-id x op]
  (add-transaction db
                   :transaction
                   {:categoryid cat-id
                    :amount (case op :increment x :decrement (- x))
                    :ts (java.time.LocalDateTime/now)})
  (j/execute! db ["update category set funds=funds-? where id=?" x cat-id])
  (j/execute! db ["update category set spent=spent+? where id =?" x cat-id]))


(defn update-name
  [db cat-id cat-name]
  (j/execute! db ["update category set name=? where id=?" cat-name cat-id]))


(defn update-monthly-limit
  [db cat-id limit]
  (j/execute! db ["update category set monthly_limit=? where id=?" limit cat-id]))


(defn reset-spent
  [db]
  (j/execute! db ["update category set spent=0"]))

(defn reinitialize-monthly-budget
  [db]
  (j/execute! db ["update category set funds=monthly_limit"]))

(defn reset-month [db]
  (reset-spent db)
  (reinitialize-monthly-budget))


;; Delete

(defn delete
  [db table tx-id]
  (j/delete! db table ["id=?" tx-id]))
