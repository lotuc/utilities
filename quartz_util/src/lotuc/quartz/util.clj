(ns lotuc.quartz.util
  (:require
   [lotuc.quartz.util.cronut :as cronut]
   [lotuc.quartz.util.job :as util.job]
   [lotuc.quartz.util.listener :as util.listener]
   [lotuc.quartz.util.scheduler :as util.scheduler]
   [lotuc.quartz.util.twarc :as twarc])
  (:import
   [org.quartz
    Job
    JobBuilder
    JobKey
    JobListener
    Matcher
    Scheduler
    Trigger
    TriggerBuilder
    TriggerKey]))

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

(defn make-scheduler
  ([] (util.scheduler/make-scheduler {}))
  ([properties] (util.scheduler/make-scheduler properties {}))
  ([properties options] (util.scheduler/make-scheduler properties options)))

(defn build-matcher ^Matcher [spec]
  (if (instance? Matcher spec) spec (twarc/matcher spec)))

(defn build-listener [spec]
  (if (instance? JobListener spec) spec (util.listener/make-listener spec)))

(defn add-job-listener [^Scheduler scheduler listener matcher]
  (-> (.getListenerManager scheduler)
      (.addJobListener ^JobListener (build-listener listener) (build-matcher matcher))))