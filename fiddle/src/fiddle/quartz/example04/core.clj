(ns fiddle.quartz.example04.core
  (:import
   [java.util Date]
   [org.quartz
    DisallowConcurrentExecution
    JobBuilder
    PersistJobDataAfterExecution
    SimpleScheduleBuilder
    TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example4

(def ^:const favorite-color "favorite color")
(def ^:const execution-count "count")

;;; PersistJobDataAfterExecution is essential
(defrecord ^{PersistJobDataAfterExecution true DisallowConcurrentExecution true} ColorJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))
          data (.. ctx (getJobDetail) (getJobDataMap))
          c (.getLong data execution-count)]
      (tap> (str "ColorJob: " job-key " executing at " (Date.)
                 "\n  favorite color is " (.getString data favorite-color)
                 "\n  execution count is " c))
      (.put data execution-count (inc c)))))

(def sched
  (-> (StdSchedulerFactory.)
      (.getScheduler)))

(.clear sched)

(def start-time (Date.))

(defn- build-job [job-name job-group opts]
  (let [job (-> (JobBuilder/newJob ColorJob)
                (.withIdentity job-name job-group)
                (.build))]
    (doto (.getJobDataMap job)
      (.put favorite-color (:favorite-color opts))
      (.put execution-count (:execution-count opts)))
    job))

(defn- build-trigger
  [trigger-name trigger-group interval-in-seconds repeat-count]
  (-> (TriggerBuilder/newTrigger)
      (.withIdentity trigger-name trigger-group)
      (.startAt start-time)
      (.withSchedule (-> (SimpleScheduleBuilder/simpleSchedule)
                         (.withIntervalInSeconds interval-in-seconds)
                         (.withRepeatCount repeat-count)))
      (.build)))

(defn- run-and-print [job trigger]
  (let [first-time (.scheduleJob sched job trigger)]
    (tap> (str (.getKey job) " (" (.getKey trigger) ")"
               " will run at " first-time
               " and repeat: " (.getRepeatCount trigger)
               " times, every " (/ (.getRepeatInterval trigger) 1000) " seconds"))))

(run-and-print
 (build-job "job1" "group1" {:favorite-color "Green" :execution-count 1})
 (build-trigger "trigger1" "group1" 3 4))

(run-and-print
 (build-job "job2" "group1" {:favorite-color "Red" :execution-count 1})
 (build-trigger "trigger2" "group1" 3 4))

(.start sched)

(comment
  (.shutdown sched true)
  ;; scheduler meta data
  (bean (.getMetaData sched)))
