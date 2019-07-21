(ns finances.config
  (:require [clojure.edn :as edn]))

(defn load!
  []
  (edn/read-string (slurp "resources/edn/config.edn")))
