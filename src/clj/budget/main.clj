(ns budget.main
  "Main entry point"
  (:require
   [budget.config :as config]
   [budget.core :refer [new-system]]
   [com.stuartsierra.component :as c])
  (:gen-class))

(defn -main [& args]
  (c/start (new-system (config/load)))
  (println "Server up and running"))
