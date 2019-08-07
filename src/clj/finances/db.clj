(ns finances.db
  (:require [clojure.java.jdbc :as jdbc]
            [clj-time.core :as clj-time]
            [clojure.string :as string]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [finances.util.core :as util]
            [finances.util.date :as util.date]
            [jdbc.pool.c3p0 :as pool]))

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

(defn row [db table identifier]
  (cond
    (integer? identifier) (first (jdbc/query db [(str "SELECT * FROM " (name table) " WHERE id=?")
                                                 identifier]))
    (map? identifier) (first (jdbc/query db [(str "SELECT * FROM " (name table) " WHERE label=?")
                                             (:label identifier)]))))

(defn all
  [db table]
  (jdbc/query db [(str "SELECT * FROM " (name table))]))

(defn category-ids->names [db]
  (let [cats (all db :category)
        ids (map :id cats)
        ns  (map :label cats)]
    (zipmap ids ns)))

(defn get-total [db col]
  (->> [(str "SELECT sum(" (name col) ") FROM category")]
       (jdbc/query db)
       first
       :sum))

(defn- previous-month [current-month]
  (if (= current-month 1) 12 (dec current-month)))

(defn get-unreported-transactions
  ([db config]
   (let [last-report-day (->> ["SELECT day FROM report order by day desc"]
                              (jdbc/query db)
                              first
                              :day)]
     (jdbc/query db ["SELECT transaction.*, category.label, category.id
                      FROM transaction
                      INNER JOIN category
                      ON category.id = transaction.categoryid
                      WHERE time > ?
                      ORDER BY time DESC"
                     last-report-day])))
  ([db config id]
   (let [last-report-day (->> ["SELECT day FROM report order by day desc"]
                              (jdbc/query db)
                              first
                              :day)]
     (jdbc/query db ["SELECT transaction.*, category.label, category.id
                      FROM transaction
                      INNER JOIN category
                      ON category.id = transaction.categoryid
                      WHERE time > ?
                      AND categoryid = ?
                      ORDER BY time DESC"
                     last-report-day
                     id]))))

(defn get-budget [db config]
  {:total-finances (get-total db :start_balance)
   :total-remaining (get-total db :balance)
   :total-spent (get-total db :spent)
   :categories (->> (all db :category)
                    (remove #(= (:label %) "Buffer"))
                    (filter (comp not :hidden))
                    (sort-by :label))
   :buffer (row db :category {:label "Buffer"})
   :category-ids->names (category-ids->names db)
   :monthly-transactions (get-unreported-transactions db config)})

(defn get-asset-transactions [db type]
  (jdbc/query db
              ["SELECT AssetTransaction.*, asset.*
                FROM assettransaction
                INNER JOIN asset
                ON assettransaction.assetid = asset.id AND asset.type=?"
               (case type :stock 1 2)]))

(defn monthly-report-missing?
  ([db config]
   (monthly-report-missing? db config (.getValue (.getMonth (util.date/today)))))
  ([db config month]
   (if (>= (.getDayOfMonth (util.date/today)) 25)
     (-> (jdbc/query db ["SELECT id from report
                          WHERE extract(month from day) = ?"
                         month])
         empty?)
     (-> (jdbc/query db ["SELECT id from report
                          WHERE extract(month from day) = ?"
                         (previous-month month)])
         empty?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Modify

;; Delete

(defn delete
  [db table id]
  (jdbc/delete! db table ["id = ?" id]))

(defn delete-category
  [db id]
  (doseq [t (jdbc/query db ["select id from transaction where categoryid = ?" id])]
    (delete db :transaction (:id t)))
  (delete db :category id))

;; Update

(defn update-row [db table update-map identifier]
  (cond
    (integer? identifier) (jdbc/update! db table update-map ["id=?" identifier])
    (map? identifier) (jdbc/update! db table update-map ["label=?" (:label identifier)])))

(defn- decrease-balance [db amount id]
  (jdbc/execute! db ["update category set balance=balance-? where id=?" amount id])
  (jdbc/execute! db ["update category set spent=spent+? where id =?" amount id]))

(defn- increase-balance [db amount id]
  (jdbc/execute! db ["update category set balance=balance+? where id=?" amount id])
  (jdbc/execute! db ["update category set spent=spent-? where id =?" amount id]))

(defn remove-transaction [db tx-id]
  (let [{:keys [categoryid amount]} (row db :transaction tx-id)]
    (increase-balance db (- amount) categoryid)
    (delete db :transaction tx-id)))

(defn update-label [db id label]
  (jdbc/execute! db ["update category set label=? where id=?" label id]))

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
    (jdbc/execute! t-db ["UPDATE category
                          SET start_balance=start_balance-?
                          WHERE id=?" amount from])
    (jdbc/execute! t-db ["update category set start_balance=start_balance+? where id=?" amount to])))
(defn transfer-spent [db from to amount]
  (jdbc/with-db-transaction [t-db db]
    (jdbc/execute! t-db ["update category set spent=spent-? where id=?" amount from])
    (jdbc/execute! t-db ["update category set spent=spent+? where id=?" amount to])))

(defn update-start-balances! [db balances]
  (jdbc/with-db-transaction [t-db db]
    (doseq [{:keys [id start-balance]} balances]
      (jdbc/execute! t-db
                     ["UPDATE category
                       SET start_balance=?
                       WHERE id=?" start-balance id]))))

;; Add

(defn add [db table row]
  (jdbc/insert! db table row))

(defn add-category
  [db label balance]
  (jdbc/insert! db
                :category
                {:label (string/capitalize label)
                 :balance balance
                 :start_balance balance
                 :spent 0}))

(defn add-row
  [db table tx]
  (jdbc/insert! db table tx))

(defn add-report
  [db file]
  (jdbc/insert! db :report {:file file
                            :day (util.date/today)}))

(defn add-asset-transaction [db tx-type id tx-date tx-buy
                             tx-shares tx-rate tx-total tx-currency]
  (add-row db
           tx-type
           {:assetid id
            :acc "ISK"
            :day (util.date/->localdate tx-date)
            :shares (util/parse-int tx-shares)
            :buy (= tx-buy "on")
            :rate (util/parse-float tx-rate)
            :total (util/parse-float tx-total)
            :currency tx-currency}))

(defn add-transaction [db id amount op]
  (jdbc/insert! db :transaction {:categoryid id
                                 :amount (case op :increment amount :decrement (- amount))
                                 :time (java.time.LocalDateTime/now)})
  (decrease-balance db amount id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Merge two categories

(defn merge-categories [db source-id dest-id]
  (let [{:keys [balance start_balance spent]} (row db :category source-id)]
    (jdbc/with-db-transaction [t-db db]
      (jdbc/execute! t-db
                     ["update transaction set categoryid = ? where categoryid = ?"
                      dest-id
                      source-id])
      (transfer-balance t-db source-id dest-id balance)
      (transfer-start-balance t-db source-id dest-id start_balance)
      (transfer-spent t-db source-id dest-id spent)
      (jdbc/delete! t-db :category ["id = ?" source-id]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Migrations

;; 2019-07-21 Merge Fund and Stock into one table
(defn stock-fund-merge-update-tables [db]
  (jdbc/execute! db ["ALTER TABLE stock ADD COLUMN type INT NOT NULL DEFAULT 1"])
  (jdbc/execute! db ["alter table stock rename TO asset"])
  (jdbc/execute! db ["alter table stocktransaction rename to assettransaction"])
  (jdbc/execute! db ["alter table assettransaction  rename column stockid to assetid"])
  (jdbc/execute! db ["alter table fundtransaction rename column fundid to assetid"]))

(defn stock-fund-merge-add-funds [db]
  (doseq [fund (all db :fund)]
    (add-row db :asset (assoc (select-keys fund [:label :tag]) :type 2))))

(defn stock-fund-merge-add-fundtransactions [db]
  (doseq [fund (all db :fundtransaction)]
    (add-row db :assettransaction
             (update
              (select-keys fund [:day :acc :buy :shares :rate :currency :total :assetid])
              :assetid #(+ % 30)))))
