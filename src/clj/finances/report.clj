(ns finances.report
  (:require
   [taoensso.timbre :as logging]
   [clojure.java.io :as io]
   [me.raynes.fs :as fs]
   [clojure.string :as string]
   [clojure.edn :as edn]
   [finances.db :as db]
   [finances.util.core :as util]
   [finances.util.date :as util.date]))

(defn generate [{:keys [db]}]
  (let [report {:transactions (map #(select-keys % [:name :amount :ts])
                                   (db/get-monthly-transactions db/pg-db-val {:salary-day 25}))
                :summary {:was (db/get-total-finances db)
                          :remaining (db/get-total-remaining db)
                          :spent (db/get-total-spent db)}
                :budget (map #(select-keys % [:name :start_balance :spent])
                             (db/get-all db :category))}]
    (db/add db :report {:file (str report)
                        :day (util.date/today)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TXT to edn

(defn budget [file]
  (->> file
       io/reader
       line-seq
       (take-while not-empty)
       rest))

(defn summary [file]
  (->> file
       io/reader
       line-seq
       (drop-while not-empty)
       rest
       (take-while not-empty)
       rest))

(defn transactions [file]
  (->> file
       io/reader
       line-seq
       (drop-while not-empty)
       rest
       (drop-while not-empty)
       rest
       rest))

(defn txt-to-edn [file]
  {:budget (map (fn [[s start spent]] {:name s
                                       :start_balance (util/parse-int start)
                                       :spent (util/parse-int spent)})
                (map #(string/split % #" ") (budget file)))
   :summary (let [[was remaining spent] (summary file)]
              {:was (util/parse-int was)
               :remaining (util/parse-int remaining)
               :spent (util/parse-int spent)})
   :transactions (map (fn [[s amount time]] {:name s
                                             :amount (util/parse-int amount)
                                             :time time})
                      (map #(string/split % #" ") (transactions file)))})

(defn add-edn-report [db file]
  (let [date (fs/base-name file :true)]
    (db/add db :report {:day (util.date/->localdate date) :file (str (txt-to-edn file))})))


(defn add-all-reports [db]
  (doseq [file (fs/list-dir "data/reports")]
    (println "adding" file)
    (add-edn-report db file)))


(defn migrate-db-report [db id]
  (let [report (db/row db :report id)]
    (spit (str (:id report) "test.txt") (:file report))
    (db/update-row db :report {:file (str (txt-to-edn (str (:id report) "test.txt")))} 89)))
