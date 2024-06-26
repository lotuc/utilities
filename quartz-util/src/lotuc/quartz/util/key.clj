(ns lotuc.quartz.util.key
  (:require
   [lotuc.quartz.util.protocols :as p])
  (:import
   [org.quartz JobKey TriggerKey]
   [org.quartz.utils Key]))

(set! *warn-on-reflection* true)

(defn- keyword->key [f k]
  (if-some [g (namespace k)]
    (f [g (name k)])
    (f (name k))))

(extend-protocol p/->Key
  JobKey
  (->trigger-key [k] (throw (ex-info "unsupported" {:key k})))
  (->job-key [k] k)
  (->key [k] (throw (ex-info "unsupported" {:key k})))

  TriggerKey
  (->trigger-key [k] k)
  (->job-key [k] (throw (ex-info "unsupported" {:key k})))
  (->key [k] (throw (ex-info "unsupported" {:key k})))

  Key
  (->trigger-key [k] (TriggerKey. (.getGroup k) (.getName k)))
  (->job-key [k] (JobKey. (.getGroup k) (.getName k)))
  (->key [k] k)

  String
  (->trigger-key [k] (TriggerKey. k))
  (->job-key [k] (JobKey. k))
  (->key [k] (throw (ex-info "unsupported" {:key k})))

  clojure.lang.Keyword
  (->trigger-key [k] (keyword->key p/->trigger-key k))
  (->job-key [k] (keyword->key p/->job-key k))
  (->key [k] (keyword->key p/->key k))

  ;; [group name]
  clojure.lang.PersistentVector
  (->trigger-key [k] (TriggerKey. (second k) (first k)))
  (->job-key [k] (JobKey. (second k) (first k)))
  (->key [k] (Key. (second k) (first k))))
