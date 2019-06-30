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

(defn generate [{:keys [db] :as config}]
  (let [report {:transactions (map #(-> %
                                        (select-keys [:label :amount :ts :note])
                                        (rename-keys {:ts :time}) ;; TODO remove this line
                                        (update :time (fn [t] (util.date/fmt-date t))))
                                   (db/get-unreported-transactions db config))
                :summary {:was (db/get-total-finances db)
                          :remaining (db/get-total-remaining db)
                          :spent (db/get-total-spent db)}
                :budget (map #(select-keys % [:label :start_balance :spent])
                             (db/all db :category))}]
    (db/add db :report {:file (str report)
                        :day (util.date/today)})))
