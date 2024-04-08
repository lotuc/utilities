(ns fiddle.quartz.example08.core
  (:import
   [java.util Date GregorianCalendar]
   [org.quartz
    DateBuilder
    JobBuilder
    SimpleScheduleBuilder
    TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]
   [org.quartz.impl.calendar AnnualCalendar]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example8

(defrecord SimpleJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))]
      (tap> (str "SimpleJob says: " job-key " executing at " (Date.))))))

(def sched (doto (.getScheduler (StdSchedulerFactory.))
             (.clear)))

(def holidays
  (doto (AnnualCalendar.)
    ;; July 4
    (.setDayExcluded (GregorianCalendar. 2005 6 5) true)
    ;; Oct 31
    (.setDayExcluded (GregorianCalendar. 2005 9 31) true)
    ;; Dec 25
    (.setDayExcluded (GregorianCalendar. 2005 11 25) true)))

(.addCalendar sched "holidays" holidays false false)

(def run-date (DateBuilder/dateOf 0 0 10 31 10))

(defn- run-and-print
  [job trigger]
  (let [first-time (.scheduleJob sched job trigger)]
    (tap> (str (.getKey job) " (" (.getKey trigger) ")"
               " will run at " first-time
               " and repeat: " (.getRepeatCount trigger)
               " times, every " (/ (.getRepeatInterval trigger) 1000) " seconds"))))

(run-and-print
 (-> (JobBuilder/newJob SimpleJob)
     (.withIdentity "job1" "group1")
     (.build))
 (-> (TriggerBuilder/newTrigger)
     (.withIdentity "trigger1" "group1")
     (.startAt run-date)
     (.withSchedule (-> (SimpleScheduleBuilder/simpleSchedule)
                        (.withIntervalInSeconds 1)
                        (.repeatForever)))
     (.modifiedByCalendar "holidays")
     (.build)))

(.start sched)

(comment
  (.shutdown sched true))
