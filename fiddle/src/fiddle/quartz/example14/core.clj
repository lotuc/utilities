(ns fiddle.quartz.example14.core
  (:import
   [org.quartz
    DateBuilder
    DateBuilder$IntervalUnit
    JobBuilder
    SimpleScheduleBuilder
    TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]))

(defrecord TriggerEchoJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))
          trigger-key (.. ctx (getTrigger) (getKey))]
      (tap> (str "TriggerEchoJob says: " job-key " " trigger-key)))))

(def sched (-> (StdSchedulerFactory. "fiddle/quartz/example14/quartz_priority.properties")
               (.getScheduler)))

(def start-time (DateBuilder/futureDate 5 DateBuilder$IntervalUnit/SECOND))

(defn build-trigger [trigger-name job interval-in-seconds priority]
  (-> (TriggerBuilder/newTrigger)
      (.withIdentity trigger-name)
      (.startAt start-time)
      (.withSchedule (-> (SimpleScheduleBuilder/simpleSchedule)
                         (.withRepeatCount 1)
                         (.withIntervalInSeconds interval-in-seconds)))
      (.forJob job)
      (#(cond-> % priority (.withPriority priority)))
      (.build)))

(def job (-> (JobBuilder/newJob TriggerEchoJob)
             (.withIdentity "TriggerEchoJob")
             (.build)))

(def trigger1 (build-trigger "Priority1Trigger5SecondRepeat" job 5 1))
(def trigger2 (build-trigger "Priority5Trigger10SecondRepeat" job 10 nil))
(def trigger3 (build-trigger "Priority10Trigger15SecondRepeat" job 15 10))

(doto sched
  (.scheduleJob job trigger1)
  (.scheduleJob trigger2)
  (.scheduleJob trigger3))

(.start sched)

(comment
  (.shutdown sched))
