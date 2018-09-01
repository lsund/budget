(ns budget.report
  (:require
   [taoensso.timbre :as logging]
   [budget.db :as db]
   [budget.util :as util]))

(defn generate [{:keys [salary-day db report-output-dir] :as config}]
    (let [filename (format "%s/%s.txt" report-output-dir (util/fmt-today))
          cat-ids->names (db/category-ids->names db)]
      (spit  filename "BUDGET:\n")
      (doseq [c (db/get-all db :category)]
        (spit filename
              (format "%s %s %s\n"
                      (:name c)
                      (:monthly_limit c)
                      (:spent c))
              :append true))
      (spit filename
            (format "\nSUMMARY:\nBudget was: %s\nTotal Remaining: %s\nTotal Spent: %s\n"
                    (db/get-total-budget db)
                    (db/get-total-remaining db)
                    (db/get-total-spent db))
            :append true)
      (spit filename "\nTRANSACTIONS:\n" :append true)
      (doseq [t (db/get-monthly-transactions db)]
        (spit filename
              (format "%s %s %s\n"
                      (cat-ids->names (:categoryid t))
                      (:amount t)
                      (util/fmt-date (:ts t)))
              :append true))
      (let [file (slurp filename)]
        (db/add-report db file)
        (logging/info file))))
