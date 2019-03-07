(ns finances.db
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [jdbc.pool.c3p0 :as pool]
            [taoensso.timbre :as logging]
            [finances.util.core :as util]
            [finances.util.date :as util.date]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Make DB Spec

;; Heroku DB Spec
(def db-uri
  (java.net.URI. (or
                  (env :database-url)
                  "postgresql://localhost:5432/finances")))

(def user-and-password
  (if (nil? (.getUserInfo db-uri))
    nil
    (clojure.string/split (.getUserInfo db-uri) #":")))

(defn make-db-spec []
  (pool/make-datasource-spec
   {:classname "org.postgresql.Driver"
    :subprotocol "postgresql"
    :user (get user-and-password 0)
    :password (get user-and-password 1)
    :subname (if (= -1 (.getPort db-uri))
               (format "//%s%s" (.getHost db-uri) (.getPath db-uri))
               (format "//%s:%s%s" (.getHost db-uri) (.getPort db-uri) (.getPath db-uri)))}))

;; Local DB Spec
(defn pg-db [config]
  {:dbtype "postgresql"
   :dbname (:name config)
   :user "postgres"})

(def pg-db-val (pg-db {:name "finances"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API

(defrecord Db [db db-config]
  component/Lifecycle

  (start [component]
    (println ";; [Db] Starting database")
    (assoc component :db (make-db-spec)))

  (stop [component]
    (println ";; [Db] Stopping database")
    component))


(defn new-db
  [config]
  (map->Db {:db-config config}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query

(defn get-total-spent [db]
  (-> (jdbc/query db ["select sum(spent) from category"]) first :sum))

(defn get-total-finances [db]
  (-> (jdbc/query db ["select sum(start_balance) from category"]) first :sum))

(defn get-total-remaining [db]
  (-> (jdbc/query db ["select sum(balance) from category"]) first :sum))

(defn- previous-month [current-month]
  (if (= current-month 1) 12 (dec current-month)))

(defn get-monthly-transactions [db {:keys [salary-day]}]
  (let [month (.getValue (util.date/finances-month salary-day))]
    (jdbc/query db ["SELECT transaction.*, category.name, category.id
                     FROM transaction
                     INNER JOIN category
                     ON category.id = transaction.categoryid
                     WHERE extract(month from ts) = ?
                     AND extract(day from ts) >= ?
                     UNION ALL
                     SELECT transaction.*, category.name, category.id
                     FROM transaction
                     INNER JOIN category
                     ON category.id = transaction.categoryid
                     WHERE extract(month from ts) = ?
                     AND extract(day from ts) <= ?"
                    (previous-month month)
                    salary-day
                    month
                    salary-day])))

(defn row [db table identifier]
  (cond
    (integer? identifier) (first (jdbc/query db [(str "SELECT * FROM " (name table) " WHERE id=?")
                                                 identifier]))
    (map? identifier) (first (jdbc/query db [(str "SELECT * FROM " (name table) " WHERE name=?")
                                             (:name identifier)]))))

(defn get-all
  ([db table]
   (get-all db table {}))
  ([db table {:keys [except]}]
   (if except
     (jdbc/query db [(str "select * from " (name table) " where name != ?") (:name except)])
     (jdbc/query db [(str "select * from " (name table))]))))

(defn category-ids->names [db]
  (let [cats (get-all db :category)
        ids (map :id cats)
        ns  (map :name cats)]
    (zipmap ids ns)))

(defn monthly-report-missing?
  ([db config]
   (monthly-report-missing? db config (.getValue (util.date/finances-month (:salary-day config)))))
  ([db config month]
   (-> (jdbc/query db ["select id from report where extract(month from day) = ?"
                       (previous-month month)])
       empty?)))

(defn get-stock-transactions [db]
  (jdbc/query db
              ["SELECT stocktransaction.*, stock.*
                FROM stocktransaction
                INNER JOIN stock
                ON stocktransaction.stockid = stock.id"]))

(defn get-fund-transactions [db]
  (jdbc/query db
              ["SELECT fundtransaction.*, fund.*
                FROM fundtransaction
                INNER JOIN fund
                ON fundtransaction.fundid = fund.id"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Modify

;; Add

(defn add [db table row]
  (jdbc/insert! db table row))

(defn add-category
  [db cat-name balance]
  (jdbc/insert! db
                :category
                {:name (util/stringify cat-name)
                 :balance balance
                 :start-balance balance
                 :spent 0}))


(defn add-report
  [db file]
  (jdbc/insert! db :report {:file file
                            :day (util.date/today)}))

;; Delete

(defn delete
  [db table id]
  (jdbc/delete! db table ["id = ?" id]))

(defn delete-category
  [db id]
  (doseq [t (jdbc/query db ["select id from transaction where categoryid = ?" id])]
    (delete db :transaction (util/parse-int (:id t))))
  (delete db :category id))

;; Update

(defn add-row
  [db table tx]
  (jdbc/insert! db table tx))

(defn update-row [db table update-map identifier]
  (cond
    (integer? identifier) (jdbc/update! db table update-map ["id=?" identifier])
    (map? identifier) (jdbc/update! db table update-map ["name=?" (:name identifier)])))

(defn- decrease-balance [db amount id]
  (jdbc/execute! db ["update category set balance=balance-? where id=?" amount id])
  (jdbc/execute! db ["update category set spent=spent+? where id =?" amount id]))

(defn- increase-balance [db amount id]
  (jdbc/execute! db ["update category set balance=balance+? where id=?" amount id])
  (jdbc/execute! db ["update category set spent=spent-? where id =?" amount id]))

(defn add-transaction [db id amount op]
  (jdbc/insert! db :transaction {:categoryid id
                                 :amount (case op :increment amount :decrement (- amount))
                                 :ts (java.time.LocalDateTime/now)})
  (decrease-balance db amount id))

(defn remove-transaction [db tx-id]
  (let [{:keys [categoryid amount]} (row db :transaction tx-id)]
    (increase-balance db (- amount) categoryid)
    (delete db :transaction tx-id)))

(defn update-name [db id cat-name]
  (jdbc/execute! db ["update category set name=? where id=?" cat-name id]))

(defn update-start-balance [db id start-balance]
  (jdbc/execute! db ["update category set start_balance=? where id=?" start-balance id]))

(defn reset-spent [db]
  (jdbc/execute! db ["update category set spent=0"]))

(defn reinitialize-monthly-finances [db]
  (jdbc/execute! db ["update category set balance=start_balance"]))

(defn reset-month [db]
  (reset-spent db)
  (reinitialize-monthly-finances db))

(defn transfer-balance [db from to amount]
  (jdbc/with-db-transaction [t-db db]
    (jdbc/execute! t-db ["update category set balance=balance-? where id=?" amount from])
    (jdbc/execute! t-db ["update category set balance=balance+? where id=?" amount to])))

(defn transfer-start-balance [db from to amount]
  (jdbc/with-db-transaction [t-db db]
    (jdbc/execute! t-db ["update category set start_balance=start_balance-? where id=?" amount from])
    (jdbc/execute! t-db ["update category set start_balance=start_balance+? where id=?" amount to])))
