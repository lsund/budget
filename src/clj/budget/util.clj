(ns budget.util
  (:require [clojure.string :as s]))

(def date-string "yyyy-MM-dd")

(defn stringify [k] (-> k name s/capitalize))

(defn parse-int [s] (Integer. (re-find  #"\d+" s)))

(defn get-current-date-header [day]
  (let [now (java.time.LocalDateTime/now)]
    (format "%s %s - %s %s %s"
            day
            (.getMonth (.plusMonths now -1))
            day
            (.getMonth now)
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


(defn is-25th? [] (= 25 (.getDayOfMonth (java.time.LocalDateTime/now))))
