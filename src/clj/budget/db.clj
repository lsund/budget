(ns budget.db
  (:require
   [clojure.java.jdbc :as j]
   [com.stuartsierra.component :as component]))


(def pg-db {:dbtype "postgresql"
            :dbname "chess"
            :host "mydb.server.com"
            :user "myuser"
            :password "secret"
            :ssl true
            :sslfactory "org.postgresql.ssl.NonValidatingFactory"})



(defrecord Db [db]
  component/Lifecycle

  (start [component]
    (println ";; [Db] Starting database")
    component)

  (stop [component]
    (println ";; [Db] Stopping database")
    component))


(defn new-db
  []
  (map->Db {}))
