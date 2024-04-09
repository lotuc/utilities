(ns lotuc.quartz.util.listener
  (:import
   [org.quartz JobListener SchedulerListener TriggerListener]))

(defmulti make-listener :type)

(defmethod make-listener :job
  [{:keys [name job-execution-vetoed job-to-be-executed job-was-executed]}]
  (reify JobListener
    (getName [_] name)
    (jobExecutionVetoed [_ context]
      (when job-execution-vetoed (job-execution-vetoed context)))
    (jobToBeExecuted [_ context]
      (when job-to-be-executed (job-to-be-executed context)))
    (jobWasExecuted [_ context exc]
      (when job-was-executed (job-was-executed context exc)))))

(defmethod make-listener :trigger
  [{:keys [name trigger-fired veto-job-execution trigger-misfired trigger-complete]}]
  (reify TriggerListener
    (getName [_] name)
    (triggerFired [_ trigger context]
      (when trigger-fired (trigger-fired trigger context)))
    (vetoJobExecution [_ trigger context]
      (when veto-job-execution (veto-job-execution trigger context)))
    (triggerMisfired [_ trigger]
      (when trigger-misfired (trigger-misfired trigger)))
    (triggerComplete [_ trigger context misfire-code]
      (when trigger-complete (trigger-complete trigger context misfire-code)))))

(defmethod make-listener :scheduler
  [{:keys [name
           job-scheduled
           job-unscheduled
           trigger-finalized
           trigger-paused
           triggers-paused
           trigger-resumed
           triggers-resumed
           job-added
           job-deleted
           job-paused
           jobs-paused
           jobs-resumed
           scheduler-error
           scheduler-in-standby-mode
           scheduler-started
           scheduler-starting
           scheduler-shutdown
           scheduler-shuttingdown
           scheduling-data-cleared]}]
  (reify SchedulerListener
    (jobScheduled [_ trigger] (when job-scheduled (job-scheduled trigger)))
    (jobUnscheduled [_ trigger-key] (when job-unscheduled (job-unscheduled trigger-key)))
    (triggerFinalized [_ trigger] (when trigger-finalized (trigger-finalized trigger)))
    (triggerPaused [_ trigger-key] (when trigger-paused (trigger-paused trigger-key)))
    (triggersPaused [_ trigger-group] (when triggers-paused (triggers-paused trigger-group)))
    (triggerResumed [_ trigger-key] (when trigger-resumed (trigger-resumed trigger-key)))
    (triggersResumed [_ trigger-group] (when triggers-resumed (triggers-resumed trigger-group)))
    (jobAdded [_ job-detail] (when job-added (job-added job-detail)))
    (jobDeleted [_ job-key] (when job-deleted (job-deleted job-key)))
    (jobPaused [_ job-key] (when job-paused (job-paused job-key)))
    (jobsPaused [_ job-group] (when jobs-paused (jobs-paused job-group)))
    (jobsResumed [_ job-group] (when jobs-resumed (jobs-resumed job-group)))
    (schedulerError [_ msg cause] (when scheduler-error (scheduler-error msg cause)))
    (schedulerInStandbyMode [_] (when scheduler-in-standby-mode (scheduler-in-standby-mode)))
    (schedulerStarted [_] (when scheduler-started (scheduler-started)))
    (schedulerStarting [_] (when scheduler-starting (scheduler-starting)))
    (schedulerShutdown [_] (when scheduler-shutdown (scheduler-shutdown)))
    (schedulerShuttingdown [_] (when scheduler-shuttingdown (scheduler-shuttingdown)))
    (schedulingDataCleared [_] (when scheduling-data-cleared (scheduling-data-cleared)))))
