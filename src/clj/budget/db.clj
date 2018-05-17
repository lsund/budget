(ns budget.db
  (:require
   [com.stuartsierra.component :as component]))


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
