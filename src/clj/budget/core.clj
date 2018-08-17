(ns budget.core
  (:require
   [com.stuartsierra.component :as c]
   [budget.app :as app]
   [budget.server :as server]
   [budget.db :as db]))

(defn new-system
  [config]
  (c/system-map :server (c/using (server/new-server (:server config))
                                 [:app])
                :app (c/using (app/new-app (:app config))
                              [:db])
                :db (c/using (db/new-db (:db config))
                             [])))
