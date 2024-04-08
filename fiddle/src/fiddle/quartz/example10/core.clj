(ns fiddle.quartz.example10.core
  (:import
   [java.util Date]
   [org.quartz.impl StdSchedulerFactory]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example10

;;; This example requires dependency: org.quartz-scheduler/quartz-jobs

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defrecord SimpleJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [job-key (.. ctx (getJobDetail) (getKey))]
      (tap> (str "SimpleJob says: " job-key " executed at " (Date.)
                 ", fired by trigger: " (.. ctx (getTrigger) (getKey))))
      (tap> (with-out-str (prn (into {} (.. ctx (getJobDetail) (getJobDataMap))))))
      (.setResult ctx "hello"))))

;;; Load the Quartz properties from a file on the classpath
(System/setProperty "org.quartz.properties" "fiddle/quartz/example10/quartz.properties")

(def sched (.getScheduler (StdSchedulerFactory.)))

(.start sched)

(comment
  (.shutdown sched true))
