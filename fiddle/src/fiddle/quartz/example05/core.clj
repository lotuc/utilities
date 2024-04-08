(ns fiddle.quartz.example05.core
  (:import
   [java.util Date]
   [org.quartz
    DateBuilder
    DisallowConcurrentExecution
    JobBuilder
    PersistJobDataAfterExecution
    SimpleScheduleBuilder
    TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example5

(def ^:const num-executions "NumExecutions")
(def ^:const execution-delay "ExecutionDelay")

;;; PersistJobDataAfterExecution is essential
(defrecord ^{PersistJobDataAfterExecution true DisallowConcurrentExecution true} StatefulDumbJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))
          data (.. ctx (getJobDetail) (getJobDataMap))
          execute-count (or (when (.containsKey data num-executions)
                              (.getLong data num-executions))
                            0)
          delay (or (when (.containsKey data execution-delay)
                      (.getLong data execution-delay))
                    5000)]
      (tap> (str "---" job-key " executing.[" (Date.) "]"))
      (.put data num-executions (inc execute-count))

      (try (Thread/sleep delay)
           (catch Exception _))

      (tap> (str "  -" job-key " complete (" execute-count ")")))))

(def sched
  (-> (StdSchedulerFactory.)
      (.getScheduler)))

(.clear sched)

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
     (.withIdentity "statefulJob1" "group1")
     (.usingJobData execution-delay 10000)
     (.build))
 (-> (TriggerBuilder/newTrigger)
     (.withIdentity "trigger1" "group1")
     (.startAt start-time)
     (.withSchedule (-> (SimpleScheduleBuilder/simpleSchedule)
                        (.withIntervalInSeconds 3)
                        (.repeatForever)))
     (.build)))

(run-and-print
 (-> (JobBuilder/newJob StatefulDumbJob)
     (.withIdentity "statefulJob2" "group1")
     (.usingJobData execution-delay 10000)
     (.build))
 (-> (TriggerBuilder/newTrigger)
     (.withIdentity "trigger2" "group1")
     (.startAt start-time)
     (.withSchedule (-> (SimpleScheduleBuilder/simpleSchedule)
                        (.withIntervalInSeconds 7)
                        (.repeatForever)
                        ;; misfire handling
                        (.withMisfireHandlingInstructionNowWithExistingCount)))
     (.build)))

(.start sched)

(comment
  (.shutdown sched true))
