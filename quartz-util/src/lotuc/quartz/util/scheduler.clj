(ns lotuc.quartz.util.scheduler
  (:require
   [lotuc.quartz.util.protocols :as p])
  (:import
   [org.quartz.impl StdSchedulerFactory]))

(def ^{:const true :private true :doc "Taken from "} quartz-default-options
  {:scheduler.rmi.export false
   :scheduler.rmi.proxy false
   :threadPool.class "org.quartz.simpl.SimpleThreadPool"
   :threadPool.threadCount 10
   :threadPool.threadPriority 5
   :threadPool.threadsInheritContextClassLoaderOfInitializingThread true
   :jobStore.misfireThreshold 60000
   :jobStore.class "org.quartz.simpl.RAMJobStore"})

(extend-protocol p/GetScheduler
  org.quartz.Scheduler
  (get-scheduler [this] this)

  org.quartz.JobExecutionContext
  (get-scheduler [this] (.getScheduler this)))

(defn make-scheduler
  ([] (make-scheduler {}))
  ([properties]
   (let [^java.util.Properties p (java.util.Properties.)]
     (doseq [[k v] (-> (merge quartz-default-options properties)
                       (update :scheduler.instanceName #(or % (str (random-uuid)))))
             :let [k (if (keyword? k)
                       (str "org.quartz." (name k))
                       (str k))
                   v (str v)]]
       (.setProperty p k v))
     (let [factory (StdSchedulerFactory. p)]
       (.getScheduler factory)))))
