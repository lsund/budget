(ns budget.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [taoensso.sente  :as sente :refer (cb-success?)]))


(enable-console-print!)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event handling


(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk"
       {:type :auto})]
  (def chsk       chsk)
  (def ch-chsk    ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))


(defmulti event-msg-handler :id)


(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (println " Event: " event)
  (event-msg-handler ev-msg))


(defmethod event-msg-handler :default
  [{:as ev-msg :keys [event]}]
  (println " Unhandled event:" event))


(defonce app-state
  (atom {:title "Budget!"}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App


(defn app
  []
  [:div {}
   [:h1.title (:title @app-state)]

   [:ul
    [:li "Hello from clojurescript"]]

   [:button#btn1 {:type
                  "button"
                  :on-click
                  (fn [e] (chsk-send! [:butget/click "test-message"]))} "Test"]

   [:div.debug app-state]])


(defn mount!
  []
  (reagent/render [app] (js/document.querySelector "#cljs-target")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Router


(def router_ " Atom to store stop function for stopping the router " (atom nil))

(defn stop-router!
  " Stop the message router by calling the previously saved stop function "
  [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  " Stop router first, then start and save the result (which is a stop callback)
in `router_ `. "
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize


(mount!)
(start-router!)
