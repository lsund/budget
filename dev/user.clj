(ns user
  (:require
   [clojure.tools.namespace.repl :as tools]
   [com.stuartsierra.component :as c]
   [finances.config :as config]
   [finances.core :refer [new-system]]))

(defn new-dev-system [] (new-system (config/load!)))

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
  (tools/refresh :after 'user/system-go!)
  (system-go!))
