(ns fiddle.quartz.example06.core
  (:import
   [java.util Date]
   [org.quartz
    DateBuilder
    DisallowConcurrentExecution
    JobBuilder
    JobExecutionException
    PersistJobDataAfterExecution
    SimpleScheduleBuilder
    TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example6

(defrecord ^{PersistJobDataAfterExecution true DisallowConcurrentExecution true}
 StatefulDumbJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))
          data (.. ctx (getJobDetail) (getJobDataMap))
          fixing? (.getBoolean data "fixing")
          denominator (.getLong data "denominator")]
      (tap> (str "---" job-key " executing at " (Date.) " with denominator " denominator))

      (try (/ 4815 denominator)
           (catch Exception e
             (tap> (str "--- Error in job!"))

             (when fixing?
               ;; fixing the problem
               (.put data "denominator" 1)
               ;; refire
               (throw (doto (JobExecutionException. e)
                        (.setRefireImmediately true))))

             (when-not fixing?
               ;; unschedule
               (throw (doto (JobExecutionException. e)
                        (.setUnscheduleAllTriggers true))))))

      (tap> (str "---" job-key " completed at " (Date.))))))

(def sched (doto (.getScheduler (StdSchedulerFactory.))
             (.clear)))

(def start-time (DateBuilder/nextGivenSecondDate nil 15))

(defn- run-and-print
  [job trigger]
  (let [first-time (.scheduleJob sched job trigger)]
    (tap> (str (.getKey job) " (" (.getKey trigger) ")"
               " will run at " first-time
               " and repeat: " (.getRepeatCount trigger)
               " times, every " (/ (.getRepeatInterval trigger) 1000) " seconds"))))

(run-and-print
 (-> (JobBuilder/newJob StatefulDumbJob)
     (.withIdentity "badJob1" "group1")
     (.usingJobData "denominator" 0)
     (.usingJobData "fixing" true)
     (.build))
 (-> (TriggerBuilder/newTrigger)
     (.withIdentity "trigger1" "group1")
     (.startAt start-time)
     (.withSchedule (-> (SimpleScheduleBuilder/simpleSchedule)
                        (.withIntervalInSeconds 10)
                        (.repeatForever)))
     (.build)))

(run-and-print
 (-> (JobBuilder/newJob StatefulDumbJob)
     (.withIdentity "badJob2" "group1")
     (.usingJobData "denominator" 0)
     (.usingJobData "fixing" false)
     (.build))
 (-> (TriggerBuilder/newTrigger)
     (.withIdentity "trigger2" "group1")
     (.startAt start-time)
     (.withSchedule (-> (SimpleScheduleBuilder/simpleSchedule)
                        (.withIntervalInSeconds 5)
                        (.repeatForever)))
     (.build)))

(.start sched)

(comment
  (.shutdown sched true))
