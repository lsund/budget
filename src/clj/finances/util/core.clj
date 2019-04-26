(ns finances.util.core
  (:require [clojure.string :as string]))

(defn parse-int [s]
  {:pre [(or (integer? s) (re-matches #"-?\d+" s))]}
  (if (integer? s)
    s
    (Integer/parseInt s)))

(defn parse-float [s]
  {:pre [(re-matches #"(-?\d+\.\d+|-?\d+)" s)]}
  (try
    (Double/parseDouble s)))
