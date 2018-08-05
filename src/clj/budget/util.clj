(ns budget.util
  (:require [clojure.string :as s]))

(defn stringify [k] (-> k name s/capitalize))

(defn parse-int [s] (Integer. (re-find  #"\d+" s)))

(defn get-current-date-header []
  (let [now (java.time.LocalDateTime/now)]
    (format "%s -> %s %s"
            (.getMonth (.plusMonths now -1))
            (.getMonth now)
            (.getYear now))))
