(ns lotuc.quartz.util.core
  (:require
   [lotuc.quartz.util.cronut :as cronut]
   [lotuc.quartz.util.job :as util.job])
  (:import
   [org.quartz
    Job
    JobBuilder
    JobKey
    Scheduler
    Trigger
    TriggerBuilder
    TriggerKey]
   [org.quartz.impl StdSchedulerFactory]))

(set! *warn-on-reflection* true)

(defn trigger-builder [config]
  (cronut/trigger-builder config))

(defn build-trigger [v]
  {:pre [(or (map? v) (instance? TriggerBuilder v) (instance? Trigger v))]}
  (cond (map? v) (.build ^TriggerBuilder (trigger-builder v))
        (instance? TriggerBuilder v) (.build ^TriggerBuilder v)
        :else v))

(defn build-job [v]
  {:pre [(or (map? v) (instance? JobBuilder v) (instance? Job v))]}
  (cond (map? v) (.build ^JobBuilder (util.job/job-builder v))
        (instance? JobBuilder v) (.build ^JobBuilder v)
        :else v))

(defn job-key [k]
  (cond (string? k) (JobKey. k)
        (vector? k) (JobKey. (first k) (second k))
        :else (do (assert (instance? JobKey k)) k)))

(defn trigger-key [k]
  (cond (string? k) (TriggerKey. k)
        (vector? k) (TriggerKey. (first k) (second k))
        :else (do (assert (instance? TriggerKey k)) k)))

(defn schedule-job
  [^Scheduler scheduler job trigger]
  (let [job (build-job job)
        trigger (build-trigger trigger)]
    (.scheduleJob scheduler job trigger)))

(defn delete-job [^Scheduler scheduler key]
  (.deleteJob scheduler (job-key key)))

(defn check-job-exists [^Scheduler scheduler key]
  (.checkExists scheduler ^JobKey (job-key key)))

(defn check-trigger-exists [^Scheduler scheduler key]
  (.checkExists scheduler ^TriggerKey (trigger-key key)))

(def ^{:private true :doc "Taken from "} quartz-default-options
  {:scheduler.rmi.export false
   :scheduler.rmi.proxy false
   :threadPool.class "org.quartz.simpl.SimpleThreadPool"
   :threadPool.threadCount 10
   :threadPool.threadPriority 5
   :threadPool.threadsInheritContextClassLoaderOfInitializingThread true
   :jobStore.misfireThreshold 60000
   :jobStore.class "org.quartz.simpl.RAMJobStore"})

(defn make-scheduler
  ([] (make-scheduler {}))
  ([properties] (make-scheduler properties {}))
  ([properties options]
   (let [n (get options :name (str (random-uuid)))
         properties' (-> (merge quartz-default-options properties)
                         (assoc :scheduler.instanceName n))
         ^java.util.Properties p
         (->> properties'
              (#(let [p (java.util.Properties.)]
                  (doseq [[k v] %
                          :let [k (if (keyword? k)
                                    (str "org.quartz." (name k))
                                    (str k))
                                v (str v)]]
                    (.setProperty p k v))
                  p)))
         factory (StdSchedulerFactory. p)]
     (.getScheduler factory))))
