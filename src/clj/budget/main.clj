(ns budget.main
  (:require
   [budget.config :as config]
   [org.httpkit.server :refer [run-server]]
   [compojure.handler :refer [site]]
   [budget.handler :refer [my-app]]))


(defn -main [& args]
  (run-server (site #'my-app) {:port (:port (config/load))})
  (println "Server up and running, port 2000."))
