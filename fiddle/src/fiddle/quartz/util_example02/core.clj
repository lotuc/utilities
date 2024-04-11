(ns fiddle.quartz.util-example02.core
  (:require
   [lotuc.quartz.util :as qu]
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as jdbc-connection])
  (:import
   [com.zaxxer.hikari HikariDataSource]
   [java.util Date]
   [org.quartz JobExecutionContext]))

;;; Setup database (MySQL)
;;;
;;; docker run --name quartz-mysql -e MYSQL_USER=quartz -e MYSQL_PASSWORD=quartz -e MYSQL_DATABASE=quartz -e MYSQL_ROOT_PASSWORD=quartz -p 3306:3306 -d mysql:8
;;;
;;; initialize table with 1 and then 2 for innodb support
;;; 1. https://github.com/elventear/quartz-scheduler/blob/master/distribution/src/main/assembly/root/docs/dbTables/tables_mysql.sql
;;; 2. https://github.com/elventear/quartz-scheduler/blob/master/distribution/src/main/assembly/root/docs/dbTables/tables_mysql_innodb.sql

(defonce ds (jdbc-connection/->pool
             HikariDataSource
             {:dbtype "mysql"
              :jdbcUrl  "jdbc:mysql://localhost:3306/quartz?allowMultiQueries=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8"
              :username "quartz"
              :password "quartz"}))

(def sched
  ;; setup datasource & create a scheduler
  (do (qu/add-connection-provider "myDS" #(.getConnection ds))
      (qu/make-scheduler {:threadPool.threadCount 4
                          :scheduler.instanceName "TestScheduler"
                          :scheduler.instanceId "instance_one"
                          :jobStore.class "org.quartz.impl.jdbcjobstore.JobStoreTX"
                          :jobStore.driverDelegateClass "org.quartz.impl.jdbcjobstore.StdJDBCDelegate"
                          :jobStore.useProperties false
                          :jobStore.dataSource "myDS"
                          :jobStore.tablePrefix "QRTZ_"
                          :jobStore.isClustered true
                          :jobStore.clusterCheckinInterval 1000})))

(defn job-fn [^JobExecutionContext ctx data-map]
  (println (str "[job-fn] " (Date.) " " (.. ctx (getJobDetail) (getKey)) " " data-map)))

;;; we do not provide connection, Quartz will get one itself
;;; the job will run normally
(qu/schedule-job sched
                 {:key ["group1" "job0"] :stateless `job-fn}
                 {:key ["group1" "trigger0"]
                  :type :simple :interval 1e3 :repeat 1})

;;; provide one connection ourself
;;; the job will run normally too
(do (jdbc/with-transaction [conn ds]
      (->> #(qu/schedule-job sched
                             {:key ["group1" "job1"] :stateless `job-fn}
                             {:key ["group1" "trigger1"]
                              :type :simple :interval 1e3 :repeat 1})
           (qu/with-connection conn)))
    ;; Since the job is committed until now, the Quartz internal signal takes no
    ;; effect (the job was not commited into store while the signal happened)
    ;;
    ;; The signaling is not necessary. Because job is already persisted, when
    ;; next signal occurs, the job will be executed.
    (qu/signal-scheduling-change sched)
    (qu/check-job-exists sched ["group1" "job1"]))

;;; provide one connection ourself
;;; but we rollback the transaction, the job won't be persisted, and won't be scheduled
(do (jdbc/with-transaction [conn ds]
      (->> #(qu/schedule-job sched
                             {:key ["group1" "job2"] :stateless `job-fn}
                             {:key ["group1" "trigger2"]
                              :type :simple :interval 1e3 :repeat 1})
           (qu/with-connection conn))
      ;; what we do
      (.rollback conn))
    (qu/check-job-exists sched ["group1" "job2"]))

(comment
  (.start sched)
  (.clear sched)
  (.shutdown sched))
