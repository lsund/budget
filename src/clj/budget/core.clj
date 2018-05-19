(ns budget.core
  (:require
   [com.stuartsierra.component :as c]
   [budget.app :as app]
   [budget.server :as server]
   [budget.db :as db]
   ,,,))

(defn new-system
  [config]
  (c/system-map

   :server
   (c/using (server/new-server 1337) [:app])

   :app
   (c/using (app/new-app) [:db])

   :db
   (c/using (db/new-db) [])))
