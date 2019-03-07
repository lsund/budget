(ns finances.report
  (:require
   [taoensso.timbre :as logging]
   [clojure.java.io :as io]
   [me.raynes.fs :as fs]
   [clojure.string :as string]
   [clojure.edn :as edn]
   [clojure.set :refer [rename-keys]]
   [finances.db :as db]
   [finances.util.core :as util]
   [finances.util.date :as util.date]))

(defn generate [{:keys [db]}]
  (let [report {:transactions (map #(-> %
                                        (select-keys [:name :amount :ts :note])
                                        (rename-keys {:ts :time}) ;; TODO remove this line
                                        (update :time (fn [t] (util.date/fmt-date t))))
                                   (db/get-monthly-transactions db/pg-db-val {:salary-day 25}))
                :summary {:was (db/get-total-finances db)
                          :remaining (db/get-total-remaining db)
                          :spent (db/get-total-spent db)}
                :budget (map #(select-keys % [:name :start_balance :spent])
                             (db/get-all db :category))}]
    (db/add db :report {:file (str report)
                        :day (util.date/today)})))
