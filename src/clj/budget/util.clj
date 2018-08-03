(ns budget.util
  (:require [clojure.string :as s]))

(defn stringify [k] (-> k name s/capitalize))

(defn parse-int [s] (Integer. (re-find  #"\d+" s)))
