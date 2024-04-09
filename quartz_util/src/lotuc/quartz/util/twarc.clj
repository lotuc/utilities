(ns lotuc.quartz.util.twarc
  (:import
   [java.util UUID]
   [org.quartz JobKey TriggerKey]
   [org.quartz.impl StdSchedulerFactory]))

;; Copyright © 2015 Andrew Rudenko
;;
;; Distributed under the Eclipse Public License either version 1.0 or (at
;; your option) any later version.
;;
;; https://github.com/prepor/twarc

(defn- uuid [] (-> (UUID/randomUUID) str))

(defn- ->properties
  [m]
  (let [p (java.util.Properties.)]
    (doseq [[k v] m]
      (.setProperty p (name k) (str v)))
    p))

(defn job-key
  ([group name] (JobKey. name group))
  ([name] (JobKey. name)))

(defn trigger-key
  ([group name] (TriggerKey. name group))
  ([name] (TriggerKey. name)))

(defn make-scheduler
  ([] (make-scheduler {}))
  ([properties] (make-scheduler properties {}))
  ([properties options]
   (let [n (get options :name (uuid))
         factory (StdSchedulerFactory.
                  (->> (assoc properties :scheduler.instanceName n)
                       (map (juxt (comp #(str "org.quartz." (name %)) first) second))
                       (->properties)))]
     (.getScheduler factory))))
