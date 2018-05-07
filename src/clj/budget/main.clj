(ns budget.main
  (:require
   [compojure.handler :refer [site]]
   [org.httpkit.server :refer [run-server]]
   [budget.core :refer [all-routes]]))

(defn -main [& args]
  (run-server (site #'all-routes) {:port 1337}))
