(ns fiddle.quartz.example01.core
  (:import
   [java.util Date]
   [org.quartz DateBuilder JobBuilder TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example1

(defrecord HelloJob []
  org.quartz.Job
  (execute [_ _ctx]
    (println "Hello Quartz! - " (Date.))))

(def sched
  (-> (StdSchedulerFactory.)
      (.getScheduler)))

(def job
  (-> (JobBuilder/newJob HelloJob)
      (.withIdentity "job1" "group1")
      (.build)))

(def trigger
  (-> (TriggerBuilder/newTrigger)
      (.withIdentity "trigger1" "group1")
      (.startAt (DateBuilder/evenSecondDate (Date.)))
      (.build)))

(.scheduleJob sched job trigger)

(.start sched)

(comment
  (.shutdown sched true))
