(ns finances.main
  "Main entry point"
  (:require
   [finances.config :as config]
   [finances.core :refer [new-system]]
   [com.stuartsierra.component :as c])
  (:gen-class))

(defn -main [& args]
  (c/start (new-system (config/load!)))
  (println "Server up and running"))
