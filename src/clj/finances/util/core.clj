(ns finances.util.core
  (:require [clojure.string :as string]))

(defn stringify [k] (-> k name string/capitalize))

(defn parse-int [s]
  {:pre [(re-matches #"-?\d+" s)]}
  (if (integer? s)
    s
    (Integer/parseInt s)))

(defn parse-float [s]
  {:pre [(re-matches #"(-?\d+\.\d+|-?\d+)" s)]}
  (try
    (Double/parseDouble s)))
