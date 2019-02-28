(ns budget.main
  (:require
   [budget.config :as config]
   [org.httpkit.server :refer [run-server]]
   [compojure.handler :refer [site]]
   [budget.handler :refer [my-app]]))

(defn -main [& args]
  (c/start (new-system (config/load)))
  (println "Server up and running"))
