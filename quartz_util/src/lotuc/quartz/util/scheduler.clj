(ns lotuc.quartz.util.scheduler
  (:import
   [org.quartz.impl StdSchedulerFactory]))

(def ^{:private true :doc "Taken from "} quartz-default-options
  {:scheduler.rmi.export false
   :scheduler.rmi.proxy false
   :threadPool.class "org.quartz.simpl.SimpleThreadPool"
   :threadPool.threadCount 10
   :threadPool.threadPriority 5
   :threadPool.threadsInheritContextClassLoaderOfInitializingThread true
   :jobStore.misfireThreshold 60000
   :jobStore.class "org.quartz.simpl.RAMJobStore"})

(defn make-scheduler
  ([] (make-scheduler {}))
  ([properties] (make-scheduler properties {}))
  ([properties options]
   (let [n (get options :name (str (random-uuid)))
         properties' (-> (merge quartz-default-options properties)
                         (assoc :scheduler.instanceName n))
         ^java.util.Properties p
         (->> properties'
              (#(let [p (java.util.Properties.)]
                  (doseq [[k v] %
                          :let [k (if (keyword? k)
                                    (str "org.quartz." (name k))
                                    (str k))
                                v (str v)]]
                    (.setProperty p k v))
                  p)))
         factory (StdSchedulerFactory. p)]
     (.getScheduler factory))))
