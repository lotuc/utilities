(ns lotuc.quartz.util.cronut
  (:require
   [lotuc.quartz.util.conversion :as cnv]
   [lotuc.quartz.util.protocols :as p])
  (:import
   [java.util TimeZone]
   [org.quartz
    CronScheduleBuilder
    SimpleScheduleBuilder
    Trigger
    TriggerBuilder]))

;; MIT License
;;
;; Copyright (c) 2022 Factor House
;;
;; Permission is hereby granted, free of charge, to any person obtaining a copy
;; of this software and associated documentation files (the "Software"), to deal
;; in the Software without restriction, including without limitation the rights
;; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
;; copies of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:
;;
;; The above copyright notice and this permission notice shall be included in all
;; copies or substantial portions of the Software.
;;
;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
;; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
;; SOFTWARE.

;;; The original source comes from: https://github.com/factorhouse/cronut
;;; Keeps only the trigger builder

(defn base-trigger-builder
  "Provide a base trigger-builder from configuration"
  [{:keys [key description start end priority data-map]}]
  (cond-> (TriggerBuilder/newTrigger)
    key (.withIdentity (p/->trigger-key key))
    description (.withDescription description)
    start (.startAt start)
    (nil? start) (.startNow)
    data-map (.usingJobData (cnv/to-job-data data-map))
    end (.endAt end)
    priority (.withPriority (int priority))))

(defn simple-schedule
  "Provide a simple schedule from configuration"
  [{:keys [interval time-unit repeat misfire]}]
  (let [schedule (SimpleScheduleBuilder/simpleSchedule)]
    (case time-unit
      :millis (.withIntervalInMilliseconds schedule interval)
      :seconds (.withIntervalInSeconds schedule interval)
      :minutes (.withIntervalInMinutes schedule interval)
      :hours (.withIntervalInHours schedule interval)
      nil (when interval (.withIntervalInMilliseconds schedule interval)))
    (case misfire
      :fire-now (.withMisfireHandlingInstructionFireNow schedule)
      :ignore (.withMisfireHandlingInstructionIgnoreMisfires schedule)
      :next-existing (.withMisfireHandlingInstructionNextWithExistingCount schedule)
      :next-remaining (.withMisfireHandlingInstructionNextWithRemainingCount schedule)
      :now-existing (.withMisfireHandlingInstructionNowWithExistingCount schedule)
      :now-remaining (.withMisfireHandlingInstructionNowWithRemainingCount schedule)
      nil nil)
    (cond
      (number? repeat) (.withRepeatCount schedule repeat)
      (= :forever repeat) (.repeatForever schedule))
    schedule))

(defn cron-schedule
  "Provide a cron schedule from configuration"
  [{:keys [cron time-zone misfire]}]
  (let [schedule (CronScheduleBuilder/cronSchedule ^String cron)]
    (case misfire
      :ignore (.withMisfireHandlingInstructionIgnoreMisfires schedule)
      :do-nothing (.withMisfireHandlingInstructionDoNothing schedule)
      :fire-and-proceed (.withMisfireHandlingInstructionFireAndProceed schedule)
      nil nil)
    (when time-zone
      (.inTimeZone schedule (TimeZone/getTimeZone ^String time-zone)))
    schedule))

(defmulti trigger-builder :type)

(defmethod trigger-builder :simple
  [config]
  (.withSchedule ^TriggerBuilder (base-trigger-builder config)
                 (simple-schedule config)))

(defmethod trigger-builder :cron
  [config]
  (.withSchedule ^TriggerBuilder (base-trigger-builder config)
                 (cron-schedule config)))

(defmethod trigger-builder :default
  [config]
  (base-trigger-builder config))

(defn shortcut-interval
  "Trigger immediately, at an interval-ms, run forever (well that's optimistic but you get the idea)"
  [interval-ms]
  (trigger-builder {:type      :simple
                    :interval  interval-ms
                    :time-unit :millis
                    :repeat    :forever}))

(defn shortcut-cron
  [cron]
  (trigger-builder {:type :cron
                    :cron cron}))

(def data-readers
  {'cronut/trigger  lotuc.quartz.util.cronut/trigger-builder
   'cronut/cron     lotuc.quartz.util.cronut/shortcut-cron
   'cronut/interval lotuc.quartz.util.cronut/shortcut-interval})

(extend-protocol p/->Trigger
  Trigger
  (->trigger [this] this)

  TriggerBuilder
  (->trigger [this] (.build this))

  clojure.lang.PersistentArrayMap
  (->trigger [spec] (p/->trigger (trigger-builder spec))))
