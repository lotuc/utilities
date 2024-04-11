(ns fiddle.quartz.example11.core
  (:import
   [java.util Date]
   [org.quartz
    DateBuilder
    DateBuilder$IntervalUnit
    JobBuilder
    TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example11

(def ^:const delay-time "delay time")

(defrecord SimpleJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))
          t (.. ctx (getJobDetail) (getJobDataMap) (getLong delay-time))]
      (tap> (str "SimpleJob says: " job-key " executed at " (Date.)))
      (try (Thread/sleep t) (catch Exception _))
      (tap> (str "Finished Executing job: " job-key " at " (Date.))))))

;;; Load the Quartz properties from a file on the classpath
(System/setProperty "org.quartz.properties" "fiddle/quartz/example11/quartz.properties")

(def sched (.getScheduler (StdSchedulerFactory.)))

(doseq [c (range 500)]
  (let [job (-> (JobBuilder/newJob SimpleJob)
                (.withIdentity (str "job" c) "group1")
                (.build))
        trigger (-> (TriggerBuilder/newTrigger)
                    (.withIdentity (str "trigger" c) "group1")
                    (.startAt (DateBuilder/futureDate (+ 10000 (* c 100)) DateBuilder$IntervalUnit/MILLISECOND))
                    (.build))]
    (-> (.getJobDataMap job)
        (.put delay-time (long (rand-int 2500))))
    (.scheduleJob sched job trigger)
    (when (zero? (mod (inc c) 5))
      (tap> (str "...scheduled " (inc c) " jobs")))))

(.start sched)

(comment
  (.shutdown sched true))
