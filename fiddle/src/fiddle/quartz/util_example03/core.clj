(ns fiddle.quartz.util-example03.core
  (:require
   [lotuc.quartz.util :as qu])
  (:import
   [java.util Date]
   [org.quartz JobExecutionContext]))

(defonce sched (qu/make-scheduler))

(.clear sched)

(defn retryable-job-fn [^JobExecutionContext ctx data-map]
  (Thread/sleep 1000)
  (let [data-map (update data-map "val" (fnil inc 0))
        c (data-map "val")
        low (or (data-map "low") 2)
        skip (or (data-map "skip") 3)
        high (or (data-map "high") 5)]

    ;; three recover strategies
    (cond
      ;; retry on new state
      (< c low)  {:recover :refire :data-map data-map}
      ;; skip current trigger's execution
      (= c skip) {:recover :unschedule-trigg :data-map data-map}
      ;; stop job's execution
      (> c high) {:recover :unschedule-all-triggs :data-map data-map}
      :else (do (println (str "[retryable-job-fn] "
                              (.. ctx (getJobDetail) (getKey))
                              " " data-map " finished at " (Date.)))
                {:data-map data-map}))))

(qu/schedule-job sched
                 {:key ["group1" "job-retry"] :stateful `retryable-job-fn
                  :data-map {"val" 0 "low" 3 "skip" 4 "high" 6}}
                 {:key ["group1" "trigger-retry"]
                  :type :simple :interval 1e3 :repeat 10})

(.start sched)

(comment
  (qu/delete-job sched ["group1" "job-retry"])
  (.clear sched)
  (.shutdown sched))
