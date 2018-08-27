(ns budget.util
  (:require [clojure.string :as s]))

(def date-string "yyyy-MM-dd")

(defn stringify [k] (-> k name s/capitalize))

(defn parse-int [s] (Integer. (re-find  #"\d+" s)))

(defn get-current-date-header [day]
  (let [now (java.time.LocalDateTime/now)
        month (budget-month)]
    (format "%s %s - %s %s %s"
            day
            (.plus month -1)
            day
            month
            (.getYear now))))

(defn string->localdate [s]
  (java.time.LocalDate/parse s (java.time.format.DateTimeFormatter/ofPattern date-string)))

(defn ->localdate
  [date]
  (cond (= (type date) java.sql.Timestamp) (.. date toLocalDateTime toLocalDate)
        (= (type date) java.sql.Date) (.toLocalDate date)
        (= (type date) java.time.LocalDate) date
        (= (type date) java.time.LocalDateTime) date
        (= (type date) java.lang.String) (string->localdate date)
        (nil? date) (throw (Exception.  "Nil argument to localdate"))
        :default (throw (Exception. (str "Unknown date type: " (type date))))))

(defn fmt-date [d]
  (.format (java.time.format.DateTimeFormatter/ofPattern date-string)
           (->localdate d)))


(defn fmt-today [] (fmt-date (java.time.LocalDateTime/now)))

(defn past-25th?
  ([]
   (past-25th? (java.time.LocalDateTime/now)))
  ([d]
   (>= (.getDayOfMonth d) 25)))

(defn today [] (java.time.LocalDateTime/now))

(defn budget-month []
  (let [now (java.time.LocalDateTime/now)]
    (if (past-25th? now)
      (.getMonth (.plusMonths now 1))
      (.getMonth now))))

(defn budget-period [day]
  (let [n (.getValue (budget-month))]
    [(java.time.LocalDate/of 2018 (- n 1) day) (java.time.LocalDate/of 2018 n day)]))
