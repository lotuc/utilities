(ns fiddle.quartz.example12.job
  (:import
   [java.util Date]))

(def message "msg")

(defrecord SimpleJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))
          msg (.. ctx (getJobDetail) (getJobDataMap) (get message))]
      (tap> (str "SimpleJob says: " job-key " executed at " (Date.)))
      (tap> (str "SimpleJob: msg: " msg)))))
