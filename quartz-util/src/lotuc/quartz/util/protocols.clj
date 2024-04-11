(ns lotuc.quartz.util.protocols)

(defprotocol GetScheduler
  (^org.quartz.Scheduler get-scheduler [_]))

(defprotocol GetListenerManager
  (^org.quartz.ListenerManager get-listener-manager [_]))

(defprotocol ->Key
  (^org.quartz.TriggerKey ->trigger-key [_])
  (^org.quartz.JobKey ->job-key [_])
  (^org.quartz.utils.Key ->key [_]))

(defprotocol ->Listener
  (->listener [_]))

(defprotocol ->Matcher
  (^org.quartz.Matcher ->matcher [_]))

(defprotocol ->Trigger
  (^org.quartz.Trigger ->trigger [_]))

(defprotocol ->Job
  (^org.quartz.Job ->job [_]))
