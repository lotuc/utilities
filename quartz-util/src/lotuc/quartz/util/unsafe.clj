(ns lotuc.quartz.util.unsafe
  (:import
   [java.lang.reflect Field]))

(set! *warn-on-reflection* true)

(defn private-field [^Object obj ^String field-name]
  (when-let [^Field f (some
                       (fn [^Class c]
                         (try (.getDeclaredField c field-name)
                              (catch NoSuchFieldException _ nil)))
                       (take-while some? (iterate (fn [^Class c] (.getSuperclass c)) (.getClass obj))))]
    (. f (setAccessible true))
    (. f (get obj))))

(defn signal-scheduling-change
  "Notice that not all scheduler supports this. And this should be only
  used for testing purpose."
  [sched]
  (when-some [^org.quartz.spi.SchedulerSignaler signaler
              (some-> sched
                      (private-field "sched")
                      (private-field "signaler"))]
    (.signalSchedulingChange signaler 0)))
