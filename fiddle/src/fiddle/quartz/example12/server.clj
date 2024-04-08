(ns fiddle.quartz.example12.server
  (:require
   [fiddle.quartz.example12.job])
  (:import
   [org.quartz.impl StdSchedulerFactory]))

;;; start a separate clojure repl & require this namespace to start the server
;;; (require '[fiddle.quartz.example12.server])

;;; Load the Quartz properties from a file on the classpath
(System/setProperty "org.quartz.properties" "fiddle/quartz/example12/server.properties")

(def sched (.getScheduler (StdSchedulerFactory.)))

(.start sched)

(comment
  (.shutdown sched true))
