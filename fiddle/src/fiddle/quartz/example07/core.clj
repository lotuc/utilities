(ns fiddle.quartz.example07.core
  (:import
   [org.quartz
    DateBuilder
    JobBuilder
    SimpleScheduleBuilder
    TriggerBuilder]
   [org.quartz.impl StdSchedulerFactory]))

;;; https://github.com/quartz-scheduler/quartz/tree/main/examples/src/main/java/org/quartz/examples/example7

(gen-class
 :name fiddle.quartz.example07.core.DumbInterruptableJob
 :implements [org.quartz.InterruptableJob]
 :state state
 :init init
 :constructors {[] []}
 :prefix "dumb-interruptable-job-")

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn dumb-interruptable-job-init []
  [[] (atom nil)])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn dumb-interruptable-job-execute [this ctx]
  (let [job-key (.. ctx (getJobDetail) (getKey))
        !state (.state this)]
    (swap! !state assoc :job-key job-key :interrupted? false)
    (tap> (str "--- " job-key " executing at " (java.util.Date.)))

    (loop [[_ & xs] (range 4)]
      (if (@!state :interrupted?)
        (tap> (str "--- " job-key " -- Interrupted... bailing out!"))
        (do
          (try (Thread/sleep 1000)
               (catch Exception _))
          (if (seq xs)
            (recur xs)
            (tap> (str "---" job-key " completed at " (java.util.Date.)))))))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn dumb-interruptable-job-interrupt [this]
  (let [!state (.state this)
        {:keys [job-key]} @!state]
    (tap> (str "--- " job-key " -- INTERRUPTING --"))
    (swap! !state assoc :interrupted? true)))

(comment
  ;; enable compiling in REPL
  ;; https://stackoverflow.com/questions/42617678/gen-class-in-clojure-and-use-it-immediately
  (do (def original-compile-files *compile-files*)
      (alter-var-root #'*compile-files* (constantly true)))
  ;; recovering compiling option
  (alter-var-root #'*compile-files* (constantly original-compile-files)))

(def sched (doto (.getScheduler (StdSchedulerFactory.))
             (.clear)))

(def start-time (DateBuilder/nextGivenSecondDate nil 15))

(defn- run-and-print
  [job trigger]
  (let [first-time (.scheduleJob sched job trigger)]
    (tap> (str (.getKey job) " (" (.getKey trigger) ")"
               " will run at " first-time
               " and repeat: " (.getRepeatCount trigger)
               " times, every " (/ (.getRepeatInterval trigger) 1000) " seconds"))))

(def job1 (-> (JobBuilder/newJob fiddle.quartz.example07.core.DumbInterruptableJob)
              (.withIdentity "interruptableJob1" "group1")
              (.build)))

(run-and-print
 job1
 (-> (TriggerBuilder/newTrigger)
     (.withIdentity "trigger1" "group1")
     (.startAt start-time)
     (.withSchedule (-> (SimpleScheduleBuilder/simpleSchedule)
                        (.withIntervalInSeconds 5)
                        (.repeatForever)))
     (.build)))

(.start sched)

(comment
  (.interrupt sched (.getKey job1))

  (.shutdown sched true))
