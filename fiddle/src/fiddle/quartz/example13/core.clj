(ns fiddle.quartz.example13.core
  (:import
   [java.util Date]
   [org.quartz
    DateBuilder
    DateBuilder$IntervalUnit
    DisallowConcurrentExecution
    JobBuilder
    PersistJobDataAfterExecution
    SimpleScheduleBuilder
    TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]))

;;; Setup database (MySQL)
;;;
;;; docker run --name quartz-mysql -e MYSQL_USER=quartz -e MYSQL_PASSWORD=quartz -e MYSQL_DATABASE=quartz -e MYSQL_ROOT_PASSWORD=quartz -p 3306:3306 -d mysql:8
;;;
;;; initialize table with 1 and then 2 for innodb support
;;; 1. https://github.com/elventear/quartz-scheduler/blob/master/distribution/src/main/assembly/root/docs/dbTables/tables_mysql.sql
;;; 2. https://github.com/elventear/quartz-scheduler/blob/master/distribution/src/main/assembly/root/docs/dbTables/tables_mysql_innodb.sql

(defn- execute-task [task-type ctx]
  (let [job-key (.. ctx (getJobDetail) (getKey))
        recovering? (.isRecovering ctx)

        data (.. ctx (getJobDetail) (getJobDataMap))
        c (if (.containsKey data "count")
            (.getLong data "count")
            0)]
    (if recovering?
      (tap> (str task-type ": " job-key " RECOVERING at " (Date.)))
      (tap> (str task-type ": " job-key " starting at " (Date.))))

    (try (Thread/sleep 30000) (catch Exception _))

    (.put data "count" (inc c))
    (tap> (str task-type ": " job-key " done at " (Date.)))
    (tap> (str "  Execution #" c))))

(defrecord SimpleRecoveryJob []
  org.quartz.Job (execute [_ ctx] (execute-task "SimpleRecoveryJob" ctx)))

(defrecord ^{PersistJobDataAfterExecution true DisallowConcurrentExecution true} SimpleRecoveryStatefulJob []
  org.quartz.Job (execute [_ ctx] (execute-task "SimpleRecoveryStatefulJob" ctx)))

(defn get-sched [properties-file]
  (.getScheduler (StdSchedulerFactory. properties-file)))

(defn schedule-job [sched c job-type]
  (let [sched-id (.getSchedulerInstanceId sched)]
    (.scheduleJob
     sched
     (-> (JobBuilder/newJob job-type)
         (.withIdentity (str "job_" c) sched-id)
         (.requestRecovery)
         (.build))
     (-> (TriggerBuilder/newTrigger)
         (.withIdentity (str "trigger_" c) sched-id)
         (.startAt (DateBuilder/futureDate 1 DateBuilder$IntervalUnit/SECOND))
         (.withSchedule (-> (SimpleScheduleBuilder/simpleSchedule)
                            (.withRepeatCount 20)
                            (.withIntervalInSeconds 5)))
         (.build)))))

(comment
  ;; start two repls
  ;; (require '[fiddle.quartz.example13.core :refer :all])

  ;; on repl1
  (def sched1 (get-sched "fiddle/quartz/example13/instance1.properties"))
  (.start sched1)

  ;; schedule two jobs
  (schedule-job sched1 1 fiddle.quartz.example13.core.SimpleRecoveryJob)
  (schedule-job sched1 2 fiddle.quartz.example13.core.SimpleRecoveryStatefulJob)
  (.getSchedulerInstanceId sched1)

  ;; try shutdown and recreate sched1, watch for job recovering
  (.shutdown sched1)
  (.clear sched1)

  ;; on another repl
  (def sched2 (get-sched "fiddle/quartz/example13/instance2.properties"))
  (.start sched2)

  (schedule-job sched2 3 fiddle.quartz.example13.core.SimpleRecoveryStatefulJob)

  (.getSchedulerInstanceId sched2)
  (.shutdown sched2)

  ;;
  )
