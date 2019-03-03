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

(defn generate [{:keys [db report-output-dir] :as config}]
  (let [filename (format "%s/%s.txt" report-output-dir (util.date/fmt-today))
        cat-ids->names (db/category-ids->names db)]
    (spit  filename "BUDGET:\n")
    (doseq [c (db/get-all db :category)]
      (spit filename
            (format "%s %s %s\n"
                    (:name c)
                    (:start_balance c)
                    (:spent c))
            :append true))
    (spit filename
          (format "\nSUMMARY:\nBudget was: %s\nTotal Remaining: %s\nTotal Spent: %s\n"
                  (db/get-total-finances db)
                  (db/get-total-remaining db)
                  (db/get-total-spent db))
          :append true)
    (spit filename "\nTRANSACTIONS:\n" :append true)
    (doseq [t (db/get-monthly-transactions db config)]
      (spit filename
            (format "%s %s %s\n"
                    (cat-ids->names (:categoryid t))
                    (:amount t)
                    (util.date/fmt-date (:ts t)))
            :append true))
    (let [file (slurp filename)]
      (db/add-report db file)
      (logging/info file))))

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
