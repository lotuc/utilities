(ns fiddle.ioc-macros.sample01-async-await
  (:refer-clojure :exclude [await])
  (:require
   [hyperfiddle.rcf :as rcf]
   [lotuc.clj.ioc-macros :as ioc])
  (:import
   [java.util.concurrent CompletableFuture]))

;;; https://github.com/leonoel/cloroutine/blob/master/doc/02-async-await.md

(defn await [_v]
  (assert nil "yield used not in (async ...) block"))

(defn await* [state blk f]
  (ioc/aset-all! state ioc/STATE-IDX blk ioc/VALUE-IDX (.get f))
  :recur)

(defmacro state-machine [& body]
  (let [terminators {`await `await*}
        crossing-env (zipmap (keys &env) (repeatedly gensym))]
    `(let [~@(mapcat (fn [[l sym]] [sym `(^:once fn* [] ~l)]) crossing-env)]
       (~(ioc/state-machine `(do ~@body) 1 [crossing-env &env] terminators)))))

(defn run-async-state-machine [state]
  (let [captured-bindings# (clojure.lang.Var/getThreadBindingFrame)
        cf (CompletableFuture.)]
    (ioc/aset-all! state ioc/BINDINGS-IDX captured-bindings#)
    (try (ioc/run-state-machine state)
         (.complete cf (ioc/aget-object state ioc/VALUE-IDX))
         (catch Throwable t
           (.completeExceptionally cf t)))
    cf))

(defmacro async [& body]
  `(let [state# (state-machine ~@body)]
     (run-async-state-machine state#)))

(rcf/tests
 (def six (async 6))
 (.get six) := 6
 (def seven (async (inc (await six))))
 (.get seven) := 7

 (def failed (async (throw (ex-info "this is fine." {}))))
 (try (.get failed) (catch Throwable _ :throw)) := :throw

 (def recovered (async (try (await failed) (catch Exception _e :failed))))
 (.get recovered) := :failed)
