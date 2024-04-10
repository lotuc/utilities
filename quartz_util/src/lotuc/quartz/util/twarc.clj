(ns lotuc.quartz.util.twarc
  (:require
   [lotuc.quartz.util.key :refer [generic-key job-key trigger-key]])
  (:import
   [org.quartz JobKey TriggerKey]
   [org.quartz.impl.matchers
    AndMatcher
    EverythingMatcher
    GroupMatcher
    KeyMatcher
    NameMatcher
    NotMatcher
    OrMatcher]))

;; Copyright © 2015 Andrew Rudenko
;;
;; Distributed under the Eclipse Public License either version 1.0 or (at
;; your option) any later version.
;;
;; https://github.com/prepor/twarc

(defn- make-fn-matcher [match-fn & [scope]]
  (reify org.quartz.Matcher
    (^boolean isMatch [_ ^org.quartz.utils.Key k]
      (and (cond (= scope :job) (instance? JobKey k)
                 (= scope :trigger) (instance? TriggerKey k)
                 :else (nil? scope))
           (match-fn (.getGroup k) (.getName k))))
    (equals [this other] (= this other))
    (hashCode [_] (hash [match-fn ::matcher]))))

(defn matcher
  "Constructor of Quartz matchers. Can be used in Liteners, for example.

  Supported matchers:

  {:fn matcher-fn :scope scope} in which matcher-fn is (fn [group name] boolean)

  {:key [\"some group\" \"some identity\"] :scope scope} where scope is :job or :trigger or nil

  {:name [:contains \"foo\"]}

  {:group [:contains \"foo\"]}

  {:and [matcher1 matcher2 ... matcherN]}

  {:or [matcher1 matcher2 ... matcherN]}

  {:not matcher}

  :everything

  :contains in :name and :group matchers also can be :equals, :ends-with and :starts-with "
  [spec]
  (cond
    (:fn spec) (make-fn-matcher (:fn spec) (:scope spec))
    (:and spec) (reduce #(AndMatcher/and (matcher %1) (matcher %2)) (:and spec))
    (:or spec) (reduce #(OrMatcher/or (matcher %1) (matcher %2)) (:or spec))
    (:not spec) (NotMatcher/not (matcher (:not spec)))
    (:group spec) (let [s (second (:group spec))]
                    (case (first (:group spec))
                      :contains (GroupMatcher/groupContains s)
                      :ends-with (GroupMatcher/groupEndsWith s)
                      :equals (GroupMatcher/groupEquals s)
                      :starts-with (GroupMatcher/groupStartsWith s)))
    (:name spec) (let [s (second (:name spec))]
                   (case (first (:name spec))
                     :contains (NameMatcher/nameContains s)
                     :ends-with (NameMatcher/nameEndsWith s)
                     :equals (NameMatcher/nameEquals s)
                     :starts-with (NameMatcher/nameStartsWith s)))
    (:key spec) (KeyMatcher/keyEquals (case (:scope spec)
                                        :job (job-key (:key spec))
                                        :trigger (trigger-key (:key spec))
                                        (generic-key (:key spec))))
    (= :everything spec) (EverythingMatcher/allJobs)))
