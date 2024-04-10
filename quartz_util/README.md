# Quartz utilities

[![Clojars Project](https://img.shields.io/clojars/v/org.lotuc/quartz-util.svg)](https://clojars.org/org.lotuc/quartz-util)
[![Clojars Project](https://img.shields.io/clojars/v/org.lotuc/quartz-util.svg?include_prereleases)](https://clojars.org/org.lotuc/quartz-util)

Prior art (and where some code snippets comes from)
- [Quartzite](https://github.com/michaelklishin/quartzite)
- [twarc](https://github.com/prepor/twarc)
- [cronut](https://github.com/factorhouse/cronut)

# Usage

```clojure
(ns fiddle.quartz.util-example01.core
  (:require
   [lotuc.quartz.util :as qu])
  (:import
   [java.util Date]
   [org.quartz JobExecutionContext]))

(defn job-fn [^JobExecutionContext ctx data-map]
  (Thread/sleep 100)
  (println (with-out-str
             (print (str "[" (.. ctx (getJobDetail) (getKey)) "]"))
             (print "[stateful] Hello Quartz!" data-map (Date.))))
  {:result "some-result"
   ;; for stateful job, this will replace the Job's data map
   ;; if you want to modify the Job's data map with `ctx`, leave this out
   :data-map (update data-map "count" (fnil inc 0))})

(def grp "group1")
(def next-job (let [!n (atom 0)] #(str "job-" (swap! !n inc))))
(def next-trigger (let [!n (atom 0)] #(str "trigger-" (swap! !n inc))))
(def job-1 (next-job))

;;; build scheduler
(defonce sched (qu/make-scheduler {:threadPool.threadCount 4}))

(.clear sched)

(qu/schedule-job sched
                 {:key [grp job-1] :stateless `job-fn}
                 {:key [grp (next-trigger)]
                  ;; run 1 & repeat 1 (total 2)
                  :type :simple :interval 1e3 :repeat 1})

(qu/schedule-job sched
                 {:key [grp (next-job)] :stateful `job-fn
                  ;; count from 5
                  :data-map {"count" 5}}
                 {:key [grp (next-trigger)]
                  ;; run 1 & repeat 3 (total 4)
                  :type :simple :interval 1e3 :repeat 3})

(defn- on-job-1-executed [ctx _]
  ;; schedule another job on `job-1` executed
  (qu/schedule-job (.getScheduler ctx)
                   {:key [grp (next-job)] :stateless `job-fn
                    :data-map {"from" "job1-listener"
                               "result" (.getResult ctx)}}
                   {:key [grp (next-trigger)]
                    :type :simple :interval 1e3 :repeat 1}))

(qu/add-listener sched
                 {:scope :job
                  :name "job1-listener"
                  :job-was-executed on-job-1-executed
                  ;; the listener matches job-1
                  :matcher {:fn (fn [group-name name] (and (= group-name grp) (= name job-1)))
                            :scope :job}})

(.start sched)
```
