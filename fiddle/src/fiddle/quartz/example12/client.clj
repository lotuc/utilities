(ns fiddle.quartz.example12.client
  (:require
   [fiddle.quartz.example12.job :refer [message]])
  (:import
   [org.quartz CronScheduleBuilder JobBuilder TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example13

;;; Load the Quartz properties from a file on the classpath
(System/setProperty "org.quartz.properties" "fiddle/quartz/example12/client.properties")

(def sched (.getScheduler (StdSchedulerFactory.)))

(def job
  (-> (JobBuilder/newJob fiddle.quartz.example12.job.SimpleJob)
      (.withIdentity "remotelyAddedJob" "default")
      (.build)))

(-> (.getJobDataMap job)
    (.put message "Your remotely added job has executed!"))

(def trigger
  (-> (TriggerBuilder/newTrigger)
      (.withIdentity "remotelyAddedTrigger" "default")
      (.forJob (.getKey job))
      (.withSchedule (CronScheduleBuilder/cronSchedule "/5 * * ? * *"))
      (.build)))

(.scheduleJob sched job trigger)

(.start sched)

(comment
  (.shutdown sched true))
