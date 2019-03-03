(ns finances.core
  (:require
   [com.stuartsierra.component :as c]
   [finances.app :as app]
   [finances.server :as server]
   [finances.db :as db]))

(defn new-system
  [config]
  (c/system-map :server (c/using (server/new-server (:server config))
                                 [:app])
                :app (c/using (app/new-app (:app config))
                              [:db])
                :db (c/using (db/new-db (:db config))
                             [])))
