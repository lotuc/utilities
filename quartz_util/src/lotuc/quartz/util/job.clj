(ns lotuc.quartz.util.job
  (:require
   [lotuc.quartz.util.conversion :as cnv]
   [lotuc.quartz.util.protocols :as p])
  (:import
   [org.quartz
    DisallowConcurrentExecution
    ExecuteInJTATransaction
    Job
    JobBuilder
    JobExecutionContext
    JobExecutionException
    PersistJobDataAfterExecution]))

(set! *warn-on-reflection* true)

(defn- base-job-builder
  [{:keys [key type description durability requests-recovery data-map]}]
  (cond-> (JobBuilder/newJob)
    key (.withIdentity (p/->job-key key))
    type (.ofType type)
    description (.withDescription description)
    data-map (.setJobData (cnv/to-job-data data-map))
    (some? durability) (.storeDurably (boolean durability))
    (some? requests-recovery) (.requestRecovery (boolean requests-recovery))))

(defn- on-function-job-response [^JobExecutionContext ctx fn-s r]
  (when (contains? r :result)
    (.setResult ctx (:result r)))
  ;; stateful job may modify the Job's data map

  (when-some [data-map (:data-map r)]
    (.. ctx (getJobDetail) (getJobDataMap) (clear))
    (.. ctx (getJobDetail) (getJobDataMap) (putAll (cnv/to-job-data data-map))))

  ;; make sure we don't lose "fn"
  (.. ctx (getJobDetail) (getJobDataMap) (put "fn" fn-s))

  (when-some [recover (:recover r)]
    (let [options #{:refire :unschedule-trigg :unschedule-all-triggs}]
      (when-not (options recover)
        (throw (JobExecutionException. (str "Invalid recover option: " recover
                                            ", expected one of " options)))))
    (let [e (JobExecutionException. (prn-str recover))]
      (case recover
        :refire (.setRefireImmediately e true)
        :unschedule-trigg (.setRefireImmediately e true)
        :unschedule-all-triggs (.setUnscheduleAllTriggers e true))
      (throw e))))

(defn execute-function-job
  [^JobExecutionContext ctx]
  (let [data-map (if (zero? (.getRefireCount ctx))
                   (.. ctx (getMergedJobDataMap))
                   ;; the merged job data map is not updated on refire execution
                   (.. ctx (getJobDetail) (getJobDataMap)))
        f (.. ctx (getJobDetail) (getJobDataMap) (get "fn"))
        f' (resolve (symbol f))
        d (-> data-map
              (cnv/from-job-data)
              (dissoc "fn"))
        r (f' ctx d)]
    (when (map? r)
      (on-function-job-response ctx f r))))

(deftype StatelessJob []
  org.quartz.Job
  (execute [_ ctx] (execute-function-job ctx)))

(deftype ^{ExecuteInJTATransaction true} StatelessJobInTx []
  org.quartz.Job
  (execute [_ ctx] (execute-function-job ctx)))

(deftype ^{DisallowConcurrentExecution true
           PersistJobDataAfterExecution true} StatefulJob []
  org.quartz.Job
  (execute [_ ctx] (execute-function-job ctx)))

(deftype ^{DisallowConcurrentExecution true
           PersistJobDataAfterExecution true
           ExecuteInJTATransaction true} StatefulJobInTx []
  org.quartz.Job
  (execute [_ ctx] (execute-function-job ctx)))

(def ^:const builtin-job-types
  {:stateless StatelessJob
   :stateless-tx StatelessJobInTx
   :stateful StatefulJob
   :stateful-tx StatefulJobInTx})

(defn job-builder
  "Build a Job from a spec map.

  If you build a customized Job class with `execute-function-job`, set
  `type` to be the Job's class & make sure data-map contains a
  stringified function var's symbol under string key `fn`."
  [{:keys [type data-map] :as spec}]
  {:pre [(or (class? type)
             (and (builtin-job-types type) (symbol? (:fn spec)))
             (some #(symbol? (% spec)) (keys builtin-job-types)))]}

  (let [[type fn-symbol]
        (cond
          (class? type) [type nil]
          (builtin-job-types type) [(builtin-job-types type) (:fn spec)]
          :else (let [k (first (filter #(symbol? (% spec)) (keys builtin-job-types)))]
                  [(builtin-job-types k) (k spec)]))
        config' (cond-> (assoc spec :type type)
                  fn-symbol (update :data-map assoc "fn" (str fn-symbol)))]
    (base-job-builder config')))

(extend-protocol p/->Job
  Job
  (->job [this] this)

  JobBuilder
  (->job [this] (.build this))

  clojure.lang.PersistentArrayMap
  (->job [spec] (p/->job (job-builder spec))))
