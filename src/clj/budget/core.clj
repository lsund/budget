(ns budget.core
  (:require
   [org.httpkit.server :refer [run-server]]
   [compojure.handler :refer [site]]
   [com.stuartsierra.component :as component]
   [compojure.route :as r]
   ,,,))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Router


(defonce router_ (atom nil))

(defn stop-router!
  "Stop router if we aware of any router stopper callback function."
  [] (when-let [stop-f @router_] (stop-f)))

(defn start-router!
  "Stop and start router while storing the router stop-function in `router_` atom."
  []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))


(defrecord App [handler]
  component/Lifecycle

  (start [component]
    (if handler
      component
      (do
        (println ";; [App] Starting, attaching handler")
        (println ";; comp: " component)
        (assoc component :handler (site #'my-app)))))

  (stop [component]
    (println ";; [App] Stopping")
    (println ";; comp: " component)
    (assoc component :handler nil)))


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

(defn new-app
  [config]
  (map->App {}))

(defn new-server
  [port]
  (map->Server {:port port}))

(defn new-system
  [config]
  (component/system-map

   :server
   (component/using (new-server 1337) [:app])

   :app
   (component/using (new-app {}) [])))
