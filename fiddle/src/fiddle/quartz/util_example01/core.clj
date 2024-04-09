(ns fiddle.quartz.util-example01.core
  (:require
   [lotuc.quartz.util :as qu])
  (:import
   [java.util Date]))

;;; a stateless job takes `data-map` (PersistentMap) & optional returns a result
(defn stateless-job [data-map]
  (println (with-out-str (print "[stateless] Hello Quartz!" data-map (Date.)))))

;;; a stateful job takes `ctx` (JobExecutionContext) & data-map (PersistentMap)
;;; & optional returns:
;;; - :result
;;; - :data-map
;;; or you can modify the result & data-map with `ctx`
(defn stateful-job [_ctx data-map]
  (println (with-out-str (print "[stateful] Hello Quartz!" data-map (Date.))))
  {:data-map (update data-map "count" (fnil inc 0))})

(def grp "group1")
(def next-job (let [!n (atom 0)] #(str "job-" (swap! !n inc))))
(def next-trigger (let [!n (atom 0)] #(str "trigger-" (swap! !n inc))))

(def job-1 (next-job))

;;; build scheduler
(def sched (qu/make-scheduler {:threadPool.threadCount 1}))

(qu/schedule-job sched
                 {:key [grp job-1]
                  :stateless `stateless-job}
                 {:key [grp (next-trigger)]
                  ;; run 1 & repeat 1 (total 2)
                  :type :simple :interval 1e3 :repeat 1})

(qu/schedule-job sched
                 {:key [grp (next-job)]
                  :stateful `stateful-job
                  ;; count from 5
                  :data-map {"count" 5}}
                 {:key [grp (next-trigger)]
                  ;; run 1 & repeat 3 (total 4)
                  :type :simple :interval 1e3 :repeat 3})

(defn- on-job-1-executed [ctx _]
  ;; schedule another job on `job-1` executed
  (qu/schedule-job (.getScheduler ctx)
                   {:key [grp (next-job)]
                    :stateless `stateless-job
                    :data-map {"from" "job1-listener"}}
                   {:key [grp (next-trigger)]
                    :type :simple :interval 1e3 :repeat 1}))

(qu/add-job-listener sched
                     {:type :job
                      :name "job1-listener"
                      :job-was-executed on-job-1-executed}
                     ;; the listener matches job-1
                     {:fn (fn [_ name] (= name job-1))})

(.start sched)

(comment
  (.shutdown sched true))
