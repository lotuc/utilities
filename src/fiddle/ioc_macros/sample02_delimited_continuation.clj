(ns fiddle.ioc-macros.sample02-delimited-continuation
  (:require
   [hyperfiddle.rcf :as rcf]
   [lotuc.clj.ioc-macros :as ioc])
  (:import
   [java.util.concurrent.atomic AtomicReferenceArray]))

;;; https://github.com/leonoel/cloroutine/blob/master/doc/04-delimited-continuations.md

(defn shift [_f & _args]
  (assert nil "yield used not in (async ...) block"))

(defn build-continuation [state blk]
  (let [s (AtomicReferenceArray. (.length state))]
    (doseq [i (range (.length state))]
      (.set s i (.get state i)))
    (fn [v]
      (ioc/aset-all! s ioc/STATE-IDX blk ioc/VALUE-IDX v)
      (ioc/run-state-machine s)
      (ioc/aget-object s ioc/VALUE-IDX))))

(defn shift* [state blk f & args]
  (let [v (apply f (build-continuation state blk) args)]
    (ioc/aset-all! state ioc/VALUE-IDX v))
  nil)

(defmacro state-machine [& body]
  (let [terminators {`shift `shift*}
        crossing-env (zipmap (keys &env) (repeatedly gensym))]
    `(let [~@(mapcat (fn [[l sym]] [sym `(^:once fn* [] ~l)]) crossing-env)]
       (~(ioc/state-machine `(do ~@body) 1 [crossing-env &env] terminators)))))

(defmacro run-state-machine [state]
  `(let [captured-bindings# (clojure.lang.Var/getThreadBindingFrame)]
     (ioc/aset-all! ~state ~ioc/BINDINGS-IDX captured-bindings#)
     (ioc/run-state-machine ~state)
     (ioc/aget-object ~state ioc/VALUE-IDX)))

(defmacro reset [& body]
  `(let [state# (state-machine ~@body)]
     (run-state-machine state#)))

(rcf/tests
 (reset (* 2 (shift map (range 3)))) := '(0 2 4))
