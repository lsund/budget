(ns budget.main
  (:require
   [org.httpkit.server :refer [run-server]]
   [compojure.handler :refer [site]]
   [budget.handler :refer [my-app]]))


(defn -main [& args]
  (run-server (site #'my-app) {:port 1337})
  (println "Server up and running, port 1337."))
