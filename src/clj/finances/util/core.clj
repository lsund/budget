(ns finances.util.core
  (:require [clojure.string :as string]))

(defn stringify [k] (-> k name string/capitalize))

(defn parse-int [s]
  (if (integer? s)
    s
    (Integer. (re-find  #"\d+" s))))

(defn parse-float [s]
  (try
    (Double. (re-find #"\d+\.\d+" s))
    (catch Exception _
      (parse-int s))))
