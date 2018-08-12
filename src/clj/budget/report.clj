(ns budget.report
  (:require
   [taoensso.timbre :as logging]
   [budget.db :as db]
   [budget.util :as util]))

(defn maybe-generate-and-reset [config]
  (when (util/is-25th?)
    (let [output-file (format "%s/%s.txt" (:report-output-dir config) (util/fmt-today))
          cat-ids->names (db/category-ids->names)]
      (spit  output-file "BUDGET:\n")
      (doseq [c (db/get-all :category)]
        (spit output-file
              (format "%s %s %s\n"
                      (:name c)
                      (:monthly_limit c)
                      (:spent c))
              :append true))
      (spit output-file
            (format "\nSUMMARY:\nBudget was: %s\nTotal Remaining: %s\nTotal Spent: %s\n"
                    (db/get-total-budget)
                    (db/get-total-remaining)
                    (db/get-total-spent))
            :append true)
      (spit output-file "\nTRANSACTIONS:\n" :append true)
      (doseq [t (db/get-monthly-transactions)]
        (spit output-file
              (format "%s %s %s\n"
                      (cat-ids->names (:categoryid t))
                      (:amount t)
                      (util/fmt-date (:ts t)))
              :append true)))
    (logging/info "Generated Report")
    (db/reset-spent)
    (logging/info "Reset spent")))
