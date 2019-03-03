(ns user
  (:require
   [figwheel-sidecar.repl-api :as f]
   [clojure.tools.namespace.repl :as tools]
   [com.stuartsierra.component :as c]
   [finances.config :as config]
   [finances.core :refer [new-system]]
   ,,,))

(defn fig-start
  "This starts the figwheel server and watch based auto-compiler."
  []
  (f/start-figwheel!))

(defn fig-stop
  "Stop the figwheel server and watch based auto-compiler."
  []
  (f/stop-figwheel!))

(defn cljs-repl
  "Launch a ClojureScript REPL that is connected to your build and host environment."
  []
  (f/cljs-repl))

(defn new-dev-system [] (new-system (config/load)))

(defonce system nil)

(defn system-init! []
  (alter-var-root #'system (constantly (new-dev-system))))

(defn system-start! []
  (alter-var-root #'system c/start))

(defn system-stop! []
  (alter-var-root #'system #(when % (c/stop %))))

(defn system-go! []
  (system-init!)
  (system-start!))

(defn system-restart! []
  (system-stop!)
  ;; (tools/refresh :after 'user/system-go!)  ; Does not work because sente is failing
  (system-go!))
