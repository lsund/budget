(ns budget.core
  (:require
   [org.httpkit.server :refer [run-server]]
   [compojure.handler :refer [site]]
   [com.stuartsierra.component :as component]
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.http-kit      :refer (get-sch-adapter)]
   [ring.middleware
    [defaults :refer [site-defaults wrap-defaults]]
    [keyword-params :refer [wrap-keyword-params]]
    [params :refer [wrap-params]]]
   [compojure.core :refer [defroutes GET POST ANY]]
   [compojure.route :as r]
   [hiccup.page :refer [html5 include-css include-js]]

   ,,,))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(defmulti event-msg-handler
  "Define multifunction to dispatch messages based on id's"
  :id)

(defmethod event-msg-handler :test-message
  [{:as ev-msg}]
  (println "Test message"))

(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event id]}]
  (println "event: " event)
  (println "id: " id))

(defmethod event-msg-handler :budget/click
  [{:as ev-msg :keys [event id]}]
  (println "click event: " event)
  (println "id: " id))

(defn event-msg-handler*
  "Log every incoming message and dispatch them."
  [{:as ev-msg :keys [id ?data event]}]
  (println "Event:" event)
  (event-msg-handler ev-msg))

(defroutes all-routes

  (GET "/" []
       (html5
        [:head
         [:title "Budget"]]
      [:body
       [:div#cljs-target]
       (apply include-js ["/js/compiled/budget.js"])
       (apply include-css ["/css/style.css"])]))

  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))

  (r/resources "/")

  (r/not-found
   (html5 "not found")))

(def my-app
  (-> all-routes wrap-keyword-params wrap-params))

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
