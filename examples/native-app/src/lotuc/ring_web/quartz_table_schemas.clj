(ns lotuc.ring-web.quartz-table-schemas
  (:require
   [malli.util :as mu])
  (:import
   [java.io ByteArrayInputStream ObjectInputStream]))

(defn- decode-job-data [job-data]
  (when job-data
    (try (with-open [i0 (ByteArrayInputStream. job-data)
                     i1 (ObjectInputStream. i0)]
           (.readObject i1))
         (catch Exception _ nil))))

(defn- decode-boolean [v] (if (string? v) (= v "1") v))

(def table-schemas
  (->> {:job-details
        [:map
         [:sched-name :string]
         [:job-name :string]
         [:job-group :string]
         [:description :string]
         [:job-class-name :string]
         [:is-durable {:decode/string decode-boolean} :boolean]
         [:is-nonconcurrent {:decode/string decode-boolean} :boolean]
         [:is-update-data {:decode/string decode-boolean} :boolean]
         [:requests-recovery {:decode/string decode-boolean} :boolean]
         [:job-data {:decode/string decode-job-data} :any]]
        :triggers
        [:map
         [:sched-name :string]
         [:trigger-name :string]
         [:trigger-group :string]
         [:job-name :string]
         [:job-group :string]
         [:description :string]
         [:next-fire-time :int]
         [:prev-fire-time :int]
         [:priority :int]
         [:trigger-state :string]
         [:trigger-type :string]
         [:start-time :int]
         [:end-time :int]
         [:calendar-name :string]
         [:misfire-instr :int]
         [:job-data {:decode/string decode-job-data} :any]]
        :simple-triggers
        [:map
         [:sched-name :string]
         [:trigger-name :string]
         [:trigger-group :string]
         [:repeat-count :int]
         [:repeat-interval :int]
         [:times-triggered :int]]
        :cron-triggers
        [:map
         [:sched-name :string]
         [:trigger-name :string]
         [:trigger-group :string]
         [:cron-expression :string]
         [:time-zone-id :string]]
        :simprop-triggers
        [:map
         [:sched-name :string]
         [:trigger-name :string]
         [:trigger-group :string]
         [:str-prop-1 :string]
         [:str-prop-2 :string]
         [:str-prop-3 :string]
         [:int-prop-1 :int]
         [:int-prop-2 :int]
         [:long-prop-1 :int]
         [:long-prop-2 :int]
         [:dec-prop-1 number?]
         [:dec-prop-2 number?]
         [:bool-prop-1 {:decode/string decode-boolean} :boolean]
         [:bool-prop-2 {:decode/string decode-boolean} :boolean]]
        :blob-triggers
        [:map
         [:sched-name :string]
         [:trigger-name :string]
         [:trigger-group :string]
         [:blob-data {:decode/string decode-job-data} :any]]
        :calenders
        [:map
         [:sched-name :string]
         [:calendar-name :string]
         [:calendar {:decode/string decode-job-data} :any]]
        :paused-trigger-grps
        [:map
         [:sched-name :string]
         [:trigger-group :string]]
        :fired-triggers
        [:map
         [:sched-name :string]
         [:entry-id :string]
         [:trigger-name :string]
         [:trigger-group :string]
         [:instance-name :string]
         [:fired-time :int]
         [:sched-time :int]
         [:priority :int]
         [:state :string]
         [:job-name :string]
         [:job-group :string]
         [:is-nonconcurrent {:decode/string decode-boolean} :boolean]
         [:requests-recovery {:decode/string decode-boolean} :boolean]]
        :scheduler-state
        [:map
         [:sched-name :string]
         [:instance-name :string]
         [:last-checkin-time :int]
         [:checkin-interval :int]]
        :locks
        [:map
         [:sched-name :string]
         [:lock-name :string]]}
       (map (fn [[k v]] [k (mu/optional-keys v)]))
       (into {})))
