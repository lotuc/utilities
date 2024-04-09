(ns lotuc.quartz.util.key
  (:import
   [org.quartz JobKey TriggerKey]
   [org.quartz.utils Key]))

(set! *warn-on-reflection* true)

(defn trigger-key ^TriggerKey [k]
  (cond (string? k) (TriggerKey. k)
        (vector? k) (TriggerKey. (second k) (first k))
        :else (do (assert (instance? TriggerKey k)) k)))

(defn job-key ^JobKey [k]
  (cond (string? k) (JobKey. k)
        (vector? k) (JobKey. (second k) (first k))
        :else (do (assert (instance? JobKey k)) k)))

(defn generic-key ^Key [k]
  (cond (vector? k) (Key. (second k) (first k))
        :else (do (assert (instance? Key k)) k)))
