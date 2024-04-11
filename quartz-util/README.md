# Quartz utilities

[![Clojars Project](https://img.shields.io/clojars/v/org.lotuc/quartz-util.svg)](https://clojars.org/org.lotuc/quartz-util)
[![Clojars Project](https://img.shields.io/clojars/v/org.lotuc/quartz-util.svg?include_prereleases)](https://clojars.org/org.lotuc/quartz-util)

Prior art (and where some code snippets comes from)
- [Quartzite](https://github.com/michaelklishin/quartzite)
- [twarc](https://github.com/prepor/twarc)
- [cronut](https://github.com/factorhouse/cronut)

# Usage

```clojure
(require '[lotuc.quartz.util :refer [make-scheduler schedule-job add-listener]])

(def sched (make-scheduler))

(defn job-fn [^org.quartz.JobExecutionContext _ctx data-map]
  (println "[job-fn] " data-map)
  {:data-map (update data-map "count" (fnil inc 0))
   :result (data-map "count")})

(schedule-job sched
              {:key ::job0 :stateful `job-fn
               :data-map {:count 0}}
              {:key ::trigger0
               :type :simple :interval 1e3 :repeat 2})

(defn listener-on [scope {:keys [type] :as m}]
  (println "[listener] " scope " " type " " (keys m)))

(doseq [scope [:job :trigger :scheduler]]
  (add-listener
   sched
   (cond-> {:scope scope
            :name (str (name scope) "-job0-listener")
            :listener-fn (partial listener-on scope)}
     (not= scope :scheduler)
     (assoc :matcher :everything))))

(.start sched)
```
