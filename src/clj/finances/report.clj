(ns finances.report
  (:require
   [taoensso.timbre :as logging]
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
