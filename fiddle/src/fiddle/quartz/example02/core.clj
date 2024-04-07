(ns fiddle.quartz.example02.core
  (:import
   [java.util Date]
   [org.quartz
    DateBuilder
    DateBuilder$IntervalUnit
    JobBuilder
    SimpleScheduleBuilder
    TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example2

(defrecord SimpleJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))]
      (tap> (str "SimpleJob says: " job-key " executed at " (Date.))))))

(def sched
  (-> (StdSchedulerFactory.)
      (.getScheduler)))

(.clear sched)

(defn- build-job [job-name job-group & [build-fn]]
  (-> (JobBuilder/newJob SimpleJob)
      (.withIdentity job-name job-group)
      (#(cond-> % build-fn build-fn))
      (.build)))

(defn- build-trigger
  [trigger-name trigger-group start-at & [build-fn]]
  (-> (TriggerBuilder/newTrigger)
      (.withIdentity trigger-name trigger-group)
      (.startAt start-at)
      (#(cond-> % build-fn build-fn))
      (.build)))

(defn- run-and-print
  [job trigger]
  (let [first-time (cond
                     ;; schedule job with given trigger
                     (and job trigger) (.scheduleJob sched job trigger)
                     ;; schedule trigger which is already associated with job
                     trigger (.scheduleJob sched trigger)
                     ;; run job immediately (add & manually trigger)
                     job (do (.addJob sched job true) ; replace=true
                             (.triggerJob sched (.getKey job))))]
    (tap> (str (cond (and job trigger) (str (.getKey job) " (" (.getKey trigger) ")")
                     trigger (str (.getJobKey trigger) " (" (.getKey trigger) ")")
                     job (.getKey job))
               (if trigger
                 (str " will run at " first-time
                      " and repeat: " (.getRepeatCount trigger)
                      " times, every " (/ (.getRepeatInterval trigger) 1000) " seconds")
                 " runs immediately")))))

;;; 0, 15, 30, 45
(def start-time (DateBuilder/nextGivenSecondDate nil 15))

(run-and-print
 (build-job "job1" "group1")
 (build-trigger "trigger1" "group1" start-time))

(run-and-print
 (build-job "job2" "group1")
 (build-trigger "trigger2" "group1" start-time))

(let [job3 (build-job "job3" "group1")]
  (run-and-print
   job3
   (build-trigger "trigger3" "group1" start-time
                  ;; run 1 & repeat 10 (total 11 times)
                  #(.withSchedule % (-> (SimpleScheduleBuilder/simpleSchedule)
                                        (.withIntervalInSeconds 40)
                                        (.withRepeatCount 10)))))
  (run-and-print
   nil
   (build-trigger "trigger3" "group2" start-time
                  ;; run 1 & repeat 10 (total 11 times)
                  #(-> %
                       (.withSchedule (-> (SimpleScheduleBuilder/simpleSchedule)
                                          (.withIntervalInSeconds 10)
                                          (.withRepeatCount 2)))
                       (.forJob job3)))))

(run-and-print
 (build-job "job4" "group1")
 (build-trigger "trigger4" "group1" start-time
                ;; run 1 & repeat 5 (total 6 times)
                #(.withSchedule % (-> (SimpleScheduleBuilder/simpleSchedule)
                                      (.withIntervalInSeconds 10)
                                      (.withRepeatCount 5)))))

(run-and-print
 (build-job "job5" "group1")
 ;; run once, five minutes in the future
 (build-trigger "trigger5" "group1" (DateBuilder/futureDate 1 (DateBuilder$IntervalUnit/MINUTE))))

(run-and-print
 (build-job "job6" "group1")
 (build-trigger "trigger6" "group1" start-time
                ;; run indefinitely, every 40 seconds
                #(.withSchedule % (-> (SimpleScheduleBuilder/simpleSchedule)
                                      (.withIntervalInSeconds 40)
                                      (.repeatForever)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(.start sched)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; schedule after scheduler started

(run-and-print
 (build-job "job7" "group1")
 (build-trigger "trigger7" "group1" start-time
  ;; repeat 20 times, every 5 minutes
                #(.withSchedule % (-> (SimpleScheduleBuilder/simpleSchedule)
                                      (.withIntervalInMinutes 5)
                                      (.withRepeatCount 20)))))

(run-and-print
 ;; Jobs added with no trigger must be durable.
 (build-job "job8" "group1" #(.storeDurably %))
 nil)

(comment
  ;; reschedule job7
  (let [trigger7 (build-trigger "trigger7" "group1" start-time
                                #(.withSchedule % (-> (SimpleScheduleBuilder/simpleSchedule)
                                                      (.withIntervalInSeconds 1)
                                                      (.withRepeatCount 10))))]
    (.rescheduleJob sched (.getKey trigger7) trigger7)))

(comment
  (.shutdown sched true)
  ;; scheduler meta data
  (bean (.getMetaData sched)))
