(ns budget.core
  (:require
   [com.stuartsierra.component :as component]
   [budget.app :as app]
   [budget.server :as server]
   [budget.db :as db]
   ,,,))

(defn new-system
  [config]
  (component/system-map

   :server
   (component/using (server/new-server 1337) [:app])

   :app
   (component/using (app/new-app) [:db])

   :db
   (component/using (db/new-db) [])))
