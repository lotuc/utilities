(ns lotuc.ring-web.quartz-tasks
  (:require
   [integrant.core :as ig]
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

(defmethod ig/init-key :quartz/scheduler [_ _]
  (doto (qu/make-scheduler {:threadPool.threadCount 4})
    (qu/schedule-job
     {:key ["lotuc" "job-42"] :stateful `job-fn}
     {:key ["lotuc" "trigger-42"]
      :type :simple :interval 10e3 :repeat 100})
    (.start)))

(defmethod ig/halt-key! :quartz/scheduler [_ s]
  (when s (.shutdown s)))
