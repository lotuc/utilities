(ns fiddle.quartz.example03.core
  (:import
   [java.util Date]
   [org.quartz CronScheduleBuilder JobBuilder TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example3

(defrecord SimpleJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))]
      (tap> (str "SimpleJob says: " job-key " executed at " (Date.))))))

(def sched
  (-> (StdSchedulerFactory.)
      (.getScheduler)))

(.clear sched)

(defn- build-job [job-name job-group]
  (-> (JobBuilder/newJob SimpleJob)
      (.withIdentity job-name job-group)
      (.build)))

(defn- build-trigger
  [trigger-name trigger-group cron-expression]
  (-> (TriggerBuilder/newTrigger)
      (.withIdentity trigger-name trigger-group)
      (.withSchedule (CronScheduleBuilder/cronSchedule cron-expression))
      (.build)))

(defn- run-and-print
  [group-name job-name trigger-name cron-expression]
  (let [job (build-job job-name group-name)
        trigger (build-trigger trigger-name group-name cron-expression)
        first-time (.scheduleJob sched job trigger)]
    (tap> (str (str (.getKey job) " (" (.getKey trigger) ")")
               " will run at " first-time
               " and repeat based on expression: " (.getCronExpression trigger)))))

(run-and-print
 "group1" "job1" "trigger1"
 ;; every 20 seconds
 "0/20 * * * * ?")

(run-and-print
 "group1" "job2" "trigger2"
 ;; every other minute (at 15 seconds past the minute)
 "15 0/2 * * * ?")

(run-and-print
 "group1" "job3" "trigger3"
 ;; every other minute but only between 8am and 5pm
 "0 0/2 8-17 * * ?")

(run-and-print
 "group1" "job4" "trigger4"
 ;; every three minutes but only between 5pm and 11pm
 "0 0/3 17-23 * * ?")

(run-and-print
 "group1" "job5" "trigger5"
 ;; 10am on the 1st and 15th days of the month
 "0 0 10am 1,15 * ?")

(run-and-print
 "group1" "job6" "trigger6"
 ;; every 30 seconds but only on Weekdays (Monday through Friday)
 "0,30 * * ? * MON-FRI")

(run-and-print
 "group1" "job7" "trigger7"
 ;; every 30 seconds but only on Weekends (Saturday and Sunday)
 "0,30 * * ? * SAT,SUN")

(.start sched)

(comment
  (.shutdown sched true)
  ;; scheduler meta data
  (bean (.getMetaData sched)))
