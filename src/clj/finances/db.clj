(ns finances.db
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [clojure.java.jdbc :as j]
            [com.stuartsierra.component :as c]
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
  c/Lifecycle

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
  (-> (j/query db ["select sum(spent) from category"]) first :sum))

(defn get-total-finances [db]
  (-> (j/query db ["select sum(limit) from category"]) first :sum))

(defn get-total-remaining [db]
  (-> (j/query db ["select sum(balance) from category"]) first :sum))

(defn- previous-month [current-month]
  (if (= current-month 1) 12 (dec current-month)))

(defn get-monthly-transactions [db {:keys [salary-day]}]
  (let [month (.getValue (util.date/finances-month salary-day))]
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
   (monthly-report-missing? db config (.getValue (util.date/finances-month (:salary-day config)))))
  ([db config month]
   (-> (j/query db ["select id from report where extract(month from day) = ?"
                    (previous-month month)])
       empty?)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Modify

;; Add

(defn add-category
  [db cat-name balance]
  (j/insert! db
             :category
             {:name (util/stringify cat-name)
              :balance balance
              :limit balance
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

(defn- decrease-balance [db amount cat-id]
  (j/execute! db ["update category set balance=balance-? where id=?" amount cat-id])
  (j/execute! db ["update category set spent=spent+? where id =?" amount cat-id]))

(defn- increase-balance [db amount cat-id]
  (j/execute! db ["update category set balance=balance+? where id=?" amount cat-id])
  (j/execute! db ["update category set spent=spent-? where id =?" amount cat-id]))

(defn add-transaction [db cat-id amount op]
  (j/insert! db :transaction {:categoryid cat-id
                              :amount (case op :increment amount :decrement (- amount))
                              :ts (java.time.LocalDateTime/now)})
  (decrease-balance db amount cat-id))

(defn remove-transaction [db tx-id]
  (let [{:keys [categoryid amount]} (row db :transaction tx-id)]
    (increase-balance db (- amount) categoryid)
    (delete db :transaction tx-id)))


(defn update-name [db cat-id cat-name]
  (j/execute! db ["update category set name=? where id=?" cat-name cat-id]))

(defn update-limit [db cat-id limit]
  (j/execute! db ["update category set limit=? where id=?" limit cat-id]))

(defn reset-spent [db]
  (j/execute! db ["update category set spent=0"]))

(defn reinitialize-monthly-finances [db]
  (j/execute! db ["update category set balance=limit"]))

(defn reset-month [db]
  (reset-spent db)
  (reinitialize-monthly-finances db))

(defn transfer-balance [db from to amount]
  (j/with-db-transaction [t-db db]
    (j/execute! t-db ["update category set balance=balance-? where id=?" amount from])
    (j/execute! t-db ["update category set balance=balance+? where id=?" amount to])))

(defn transfer-limit [db from to amount]
  (j/with-db-transaction [t-db db]
    (j/execute! t-db ["update category set limit=limit-? where id=?" amount from])
    (j/execute! t-db ["update category set limit=limit+? where id=?" amount to])))
