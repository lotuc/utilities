(ns lotuc.quartz.util.conversion
  (:import
   [clojure.lang IPersistentMap]
   [org.quartz JobDataMap JobExecutionContext]))

;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

;; Monger and other ClojureWerkz project integration extension point. MK.

(defprotocol JobDataMapConversion
  (^org.quartz.JobDataMap
    to-job-data [input] "Instantiates a JobDataMap instance from a Clojure map")
  (from-job-data [input] "Converts a JobDataMap to a Clojure map"))

(defn- convert-keys-to-strings
  "Converts keys of a map to strings. Doesn't modify nested maps"
  [map]
  (->> (for [[k v] map]
         (if (keyword? k)
           [(name k) v]
           [(str k) v]))
       (into {})))

(extend-protocol JobDataMapConversion
  IPersistentMap
  (to-job-data [^clojure.lang.IPersistentMap input]
    (JobDataMap. (convert-keys-to-strings input)))

  JobDataMap
  (from-job-data [^JobDataMap input]
    (into {} (convert-keys-to-strings input)))

  JobExecutionContext
  (from-job-data [^JobExecutionContext input]
    (from-job-data (.getMergedJobDataMap input))))
