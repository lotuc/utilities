(ns lotuc.quartz.util
  (:require
   [lotuc.quartz.util.conversion :as cnv]
   [lotuc.quartz.util.cronut]
   [lotuc.quartz.util.jdbc :as jdbc]
   [lotuc.quartz.util.job]
   [lotuc.quartz.util.key]
   [lotuc.quartz.util.listener :as util.listener]
   [lotuc.quartz.util.protocols :as p]
   [lotuc.quartz.util.scheduler :as util.scheduler]
   [lotuc.quartz.util.twarc]
   [lotuc.quartz.util.unsafe :as util.unsafe]))

(set! *warn-on-reflection* true)

(defn job-key ^org.quartz.JobKey [k]
  (p/->job-key k))

(defn trigger-key ^org.quartz.TriggerKey [k]
  (p/->trigger-key k))

(defn build-trigger ^org.quartz.Trigger [spec]
  (p/->trigger spec))

(defn build-job ^org.quartz.Job [spec]
  (p/->job spec))

(defn build-listener [spec]
  (p/->listener spec))

(defn build-matcher ^org.quartz.Matcher [spec]
  (p/->matcher spec))

(defn make-scheduler
  (^org.quartz.Scheduler [] (util.scheduler/make-scheduler))
  (^org.quartz.Scheduler [properties] (util.scheduler/make-scheduler properties)))

(defn schedule-job
  [scheduler job trigger]
  (let [scheduler (p/get-scheduler scheduler)
        job (build-job job)
        trigger (build-trigger trigger)]
    (.scheduleJob scheduler job trigger)))

(defn trigger-job
  ([scheduler key]
   (.triggerJob (p/get-scheduler scheduler) (p/->job-key key)))
  ([scheduler key data-map]
   (.triggerJob (p/get-scheduler scheduler) (p/->job-key key) (cnv/to-job-data data-map))))

(defn signal-scheduling-change
  [scheduler]
  (util.unsafe/signal-scheduling-change scheduler))

(defn delete-job [scheduler key]
  (.deleteJob (p/get-scheduler scheduler) (p/->job-key key)))

(defn check-job-exists [scheduler key]
  (.checkExists (p/get-scheduler scheduler) (p/->job-key key)))

(defn check-trigger-exists [scheduler key]
  (.checkExists (p/get-scheduler scheduler) (p/->trigger-key key)))

(defn add-listener
  ([listener-manager-gettable {:keys [scope matcher] :as listener}]
   (add-listener listener-manager-gettable listener matcher))
  ([listener-manager-gettable listener matcher]
   (let [m (p/get-listener-manager listener-manager-gettable)]
     (if matcher
       (util.listener/add-listener m listener matcher)
       (util.listener/add-listener m listener)))))

(defn with-connection
  "Run scheduler operations with a managed connection."
  [^java.sql.Connection conn f]
  (binding [jdbc/*connection* conn]
    (f)))

(defn add-connection-provider [name get-connection]
  (jdbc/add-connection-provider name get-connection))
