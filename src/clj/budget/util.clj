(ns budget.util
  (:require [clojure.string :as s]))

(defn stringify [k] (-> k name s/capitalize))

(defn parse-int [s] (Integer. (re-find  #"\d+" s)))

(defn get-current-date-header [day]
  (let [now (java.time.LocalDateTime/now)]
    (format "%s %s -> %s %s %s"
            day
            (.getMonth (.plusMonths now -1))
            day
            (.getMonth now)
            (.getYear now))))
