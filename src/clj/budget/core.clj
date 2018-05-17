(ns budget.core
  (:require
   [com.stuartsierra.component :as component]
   [budget.app :as app]
   [budget.server :as server]
   ,,,))


(defn new-app
  [config]
  (app/map->App {}))


(defn new-server
  [port]
  (server/map->Server {:port port}))


(defn new-system
  [config]
  (component/system-map

   :server
   (component/using (new-server 1337) [:app])

   :app
   (component/using (new-app {}) [])))
