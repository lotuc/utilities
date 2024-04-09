(ns lotuc.quartz.util.job
  (:require
   [lotuc.quartz.util.conversion :as cnv]
   [lotuc.quartz.util.key :refer [job-key]])
  (:import
   [org.quartz
    DisallowConcurrentExecution
    JobBuilder
    JobKey
    PersistJobDataAfterExecution]))

(set! *warn-on-reflection* true)

(defn- base-job-builder
  [{:keys [key type description durability requests-recovery data-map]}]
  (cond-> (JobBuilder/newJob)
    key (.withIdentity ^JobKey (job-key key))
    type (.ofType type)
    description (.withDescription description)
    data-map (.setJobData (cnv/to-job-data data-map))
    (some? durability) (.storeDurably (boolean durability))
    (some? requests-recovery) (.requestRecovery (boolean requests-recovery))))

(deftype StatelessJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [f (.. ctx (getJobDetail) (getJobDataMap) (get "fn"))
          f' (resolve (symbol f))
          d (-> (.getMergedJobDataMap ctx)
                (cnv/from-job-data)
                (dissoc "fn"))
          r (f' d)]
      (.setResult ctx r))))

(deftype ^{DisallowConcurrentExecution true
           PersistJobDataAfterExecution true} StatefulJob []
  org.quartz.Job
  (execute [_ ctx]
    (let [f (.. ctx (getJobDetail) (getJobDataMap) (get "fn"))
          f' (resolve (symbol f))
          d (-> (.getMergedJobDataMap ctx)
                (cnv/from-job-data)
                (dissoc "fn"))
          r (f' ctx d)]
      (when (contains? r :result)
        (.setResult ctx (:result r)))
      (when (contains? r :data-map)
        (.. ctx (getJobDetail) (getJobDataMap) (clear))
        (.. ctx (getJobDetail) (getJobDataMap) (putAll (cnv/to-job-data (:data-map r)))))
      ;; make sure we don't lose "fn"
      (.. ctx (getJobDetail) (getJobDataMap) (put "fn" f)))))

(defn job-builder [{:keys [type stateful stateless data-map] :as config}]
  {:pre [(or (class? type)
             (and (#{:stateful :stateless} type)
                  (symbol? (:fn config)))
             (symbol? stateful)
             (symbol? stateless))]}
  (let [[type fn-symbol]
        (cond
          (class? type) [type nil]

          (= type :stateless) [StatelessJob (:fn config)]
          stateless [StatelessJob stateless]

          (= type :stateful) [StatefulJob (:fn config)]
          stateful [StatefulJob stateful])

        config' (cond-> (assoc config :type type)
                  fn-symbol (update :data-map assoc "fn" (str fn-symbol)))]
    (base-job-builder config')))
