(ns budget.server
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]))

(defrecord Server [app port server]
  component/Lifecycle

  (start [component]
    (println ";; [Server] Starting HttpKit on port" port)
    (if server
      component
      (do
        (println ";; comp: " component)
        (->> (run-server (:handler app) {:port port})
             (assoc component :server)))))

  (stop [component]
    (if-not server
      component
      (do
        (println ";; [Server] Stopping HttpKit")
        (println ";; comp: " component)
        (server :timeout 10)
        (assoc component :server nil)))))
