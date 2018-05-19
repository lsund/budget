(ns budget.db
  (:require
   [clojure.java.jdbc :as j]
   [com.stuartsierra.component :as c]))


(def pg-db {:dbtype "postgresql"
            :dbname "budget"
            :user "postgres"})

(def test-q (j/query pg-db
                     ["select * from asset"]))


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
