(ns lotuc.quartz.util.listener
  (:require
   [lotuc.quartz.util.protocols :as p])
  (:import
   [org.quartz JobListener SchedulerListener TriggerListener]))

(extend-protocol p/GetListenerManager
  org.quartz.Scheduler
  (get-listener-manager [scheduler]
    (.getListenerManager scheduler))

  org.quartz.JobExecutionContext
  (get-scheduler [this] (.getListenerManager (.getScheduler this))))

(defmulti make-listener :scope)

(defmethod make-listener :job
  [{:keys [name listener-fn]}]
  (reify JobListener
    (getName [_] name)
    (jobExecutionVetoed [_ context]
      (listener-fn {:context context :type :job-execution-vetoed}))
    (jobToBeExecuted [_ context]
      (listener-fn {:context context :type :job-to-be-executed}))
    (jobWasExecuted [_ context exc]
      (listener-fn {:context context :type :job-was-executed :exception exc}))))

(defmethod make-listener :trigger
  [{:keys [name listener-fn]}]
  (reify TriggerListener
    (getName [_] name)
    (triggerFired [_ trigger context]
      (listener-fn {:trigger trigger :type :trigger-fired
                    :context context}))
    (vetoJobExecution [_ trigger context]
      (listener-fn {:trigger trigger :type :veto-job-execution
                    :context context}))
    (triggerComplete [_ trigger context misfire-code]
      (listener-fn {:trigger trigger :type :trigger-complete
                    :context context :misfire-code misfire-code}))
    (triggerMisfired [_ trigger]
      (listener-fn {:trigger trigger :type :trigger-misfired}))))

(defmethod make-listener :scheduler
  [{:keys [name listener-fn]}]
  (reify SchedulerListener
    (jobScheduled [_ trigger]
      (listener-fn {:type :job-scheduled
                    :trigger trigger}))
    (triggerFinalized [_ trigger]
      (listener-fn {:type :trigger-finalized
                    :trigger trigger}))
    (jobUnscheduled [_ trigger-key]
      (listener-fn {:type :job-unscheduled
                    :trigger-key trigger-key}))
    (triggerPaused [_ trigger-key]
      (listener-fn {:type :trigger-paused
                    :trigger-key trigger-key}))
    (triggersPaused [_ trigger-group]
      (listener-fn {:type :triggers-paused
                    :trigger-group trigger-group}))
    (triggerResumed [_ trigger-key]
      (listener-fn {:type :trigger-resumed
                    :trigger-key trigger-key}))
    (triggersResumed [_ trigger-group]
      (listener-fn {:type :triggers-resumed
                    :trigger-group trigger-group}))
    (jobAdded [_ job-detail]
      (listener-fn {:type :job-added
                    :job-detail job-detail}))
    (jobDeleted [_ job-key]
      (listener-fn {:type :job-deleted
                    :job-key job-key}))
    (jobPaused [_ job-key]
      (listener-fn {:type :job-paused
                    :job-key job-key}))
    (jobsPaused [_ job-group]
      (listener-fn {:type :jobs-paused
                    :job-group job-group}))
    (jobsResumed [_ job-group]
      (listener-fn {:type :jobs-resumed
                    :job-group job-group}))
    (schedulerError [_ msg cause]
      (listener-fn {:type :scheduler-error
                    :msg msg :cause cause}))
    (schedulerInStandbyMode [_]
      (listener-fn {:type :scheduler-in-standby-mode}))
    (schedulerStarted [_]
      (listener-fn {:type :scheduler-started}))
    (schedulerStarting [_]
      (listener-fn {:type :scheduler-starting}))
    (schedulerShutdown [_]
      (listener-fn {:type :scheduler-shutdown}))
    (schedulerShuttingdown [_]
      (listener-fn {:type :scheduler-shuttingdown}))
    (schedulingDataCleared [_]
      (listener-fn {:type :scheduling-data-cleared}))))

(defn- add-listener*
  ([^org.quartz.ListenerManager m listener]
   (cond
     (instance? JobListener listener) (.addJobListener m ^JobListener listener)
     (instance? TriggerListener listener) (.addTriggerListener m ^TriggerListener listener)
     (instance? SchedulerListener listener) (.addSchedulerListener m ^SchedulerListener listener)
     :else (throw (ex-info (str "unkown listener: " listener) {:listener listener}))))
  ([^org.quartz.ListenerManager m listener ^org.quartz.Matcher matcher]
   (cond
     (instance? JobListener listener) (.addJobListener m ^JobListener listener matcher)
     (instance? TriggerListener listener) (.addTriggerListener m ^TriggerListener listener matcher)
     :else (throw (ex-info (str "unkown listener: " listener) {:listener listener})))))

(defn add-listener
  [^org.quartz.ListenerManager m listener & [matcher :as args]]
  (if (map? listener)
    (apply add-listener m (make-listener listener) args)
    (if matcher
      (add-listener* m listener (p/->matcher matcher))
      (add-listener* m listener))))
