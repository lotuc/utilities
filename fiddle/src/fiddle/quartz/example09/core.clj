(ns fiddle.quartz.example09.core
  (:import
   [java.util Date]
   [org.quartz JobBuilder SchedulerException TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]
   [org.quartz.impl.matchers KeyMatcher]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example9

(defrecord SimpleJob1 []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))]
      (tap> (str "SimpleJob1 says: " job-key " executing at " (Date.))))))

(defrecord SimpleJob2 []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))]
      (tap> (str "SimpleJob2 says: " job-key " executing at " (Date.))))))

(defrecord Job1Listener []
  org.quartz.JobListener
  (getName [_] "job1_to_job2")
  (jobToBeExecuted [_ _ctx] (tap> "Job1Listener says: Job Is about to be executed."))
  (jobExecutionVetoed [_ _ctx] (tap> "Job1Listener says: Job Execution was vetoed."))
  (jobWasExecuted [_ ctx _exception]
    (tap> "Job1Listener says: Job was executed.")
    (let [scheduler (.getScheduler ctx)]
      (try (.scheduleJob
            scheduler
            (-> (JobBuilder/newJob SimpleJob2)
                (.withIdentity "job2")
                (.build))
            (-> (TriggerBuilder/newTrigger)
                (.withIdentity "job2Trigger")
                (.build)))
           (catch SchedulerException e
             (tap> "Unable to schedule job2!")
             (println e))))))

(def sched (doto (.getScheduler (StdSchedulerFactory.))
             (.clear)))

(def job1
  (-> (JobBuilder/newJob SimpleJob1)
      (.withIdentity "job1")
      (.build)))

(def trigger1
  (-> (TriggerBuilder/newTrigger)
      (.withIdentity "trigger1")
      (.startNow)
      (.build)))

(def listener1
  (Job1Listener.))

(-> (.getListenerManager sched)
    (.addJobListener listener1 (KeyMatcher/keyEquals (.getKey job1))))

(.scheduleJob sched job1 trigger1)

(.start sched)

(comment
  (.shutdown sched true)
  (bean (.getMetaData sched)))
