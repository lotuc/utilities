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

(defn execute-function-job
  [stateful? ^JobExecutionContext ctx]
  (let [f (.. ctx (getJobDetail) (getJobDataMap) (get "fn"))
        f' (resolve (symbol f))
        d (-> (.getMergedJobDataMap ctx)
              (cnv/from-job-data)
              (dissoc "fn"))
        r (f' ctx d)]
    (when (map? r)
      (when (contains? r :result)
        (.setResult ctx (:result r)))
      ;; stateful job may modify the Job's data map
      (when (and stateful? (contains? r :data-map))
        (.. ctx (getJobDetail) (getJobDataMap) (clear))
        (.. ctx (getJobDetail) (getJobDataMap) (putAll (cnv/to-job-data (:data-map r))))))
    ;; make sure we don't lose "fn"
    (.. ctx (getJobDetail) (getJobDataMap) (put "fn" f))))

(deftype StatelessJob []
  org.quartz.Job
  (execute [_ ctx] (execute-function-job false ctx)))

(deftype ^{ExecuteInJTATransaction true} StatelessJobInTx []
  org.quartz.Job
  (execute [_ ctx] (execute-function-job false ctx)))

(deftype ^{DisallowConcurrentExecution true
           PersistJobDataAfterExecution true} StatefulJob []
  org.quartz.Job
  (execute [_ ctx] (execute-function-job true ctx)))

(deftype ^{DisallowConcurrentExecution true
           PersistJobDataAfterExecution true
           ExecuteInJTATransaction true} StatefulJobInTx []
  org.quartz.Job
  (execute [_ ctx] (execute-function-job true ctx)))

(defn job-builder
  "Build a Job from a spec map.

  If you build a customized Job class with `execute-function-job`, set
  `type` to be the Job's class & make sure data-map contains a
  stringified function var's symbol under string key `fn`."
  [{:keys [type stateful stateless stateful-tx stateless-tx data-map] :as spec}]
  {:pre [(or (class? type)
             (and (#{:stateful :stateless :stateful-tx :stateless-tx} type)
                  (symbol? (:fn spec)))
             (symbol? stateful)
             (symbol? stateless)
             (symbol? stateful-tx)
             (symbol? stateless-tx))]}
  (let [[type fn-symbol]
        (cond
          (class? type) [type nil]

          (= type :stateless) [StatelessJob (:fn spec)]
          stateless [StatelessJob stateless]

          (= type :stateless-tx) [StatelessJobInTx (:fn spec)]
          stateless-tx [StatelessJobInTx stateless-tx]

          (= type :stateful) [StatefulJob (:fn spec)]
          stateful [StatefulJob stateful]

          (= type :stateful-tx) [StatefulJob (:fn spec)]
          stateful-tx [StatefulJobInTx stateful-tx])

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
