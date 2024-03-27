(ns fiddle.ioc-macros.sample0-generators-as-lazy-sequence
  (:require
   [lotuc.clj.ioc-macros :as ioc]
   [hyperfiddle.rcf :as rcf]))

;;; https://github.com/leonoel/cloroutine/blob/master/doc/01-generators.md

(defn yield [_v]
  (assert nil "yield used not in (generator ...) block"))

(defn yield* [state blk v]
  (ioc/aset-all! state ioc/STATE-IDX blk ioc/VALUE-IDX v)
  nil)

(defmacro run-state-machine [state]
  `(let [captured-bindings# (clojure.lang.Var/getThreadBindingFrame)]
     (ioc/aset-all! ~state ~ioc/BINDINGS-IDX captured-bindings#)
     (ioc/run-state-machine ~state)
     (ioc/aget-object ~state ioc/VALUE-IDX)))

(defmacro state-machine [& body]
  (let [terminators {`yield `yield*}
        crossing-env (zipmap (keys &env) (repeatedly gensym))]
    `(let [~@(mapcat (fn [[l sym]] [sym `(^:once fn* [] ~l)]) crossing-env)]
       (~(ioc/state-machine `(do ~@body) 0 [crossing-env &env] terminators)))))

(defn state-machine->lazy-seq
  ([state]
   (lazy-seq
    (let [v (run-state-machine state)]
      (if (= v ::end)
        nil
        (cons v (state-machine->lazy-seq state)))))))

(defmacro generator [& body]
  `(let [state# (state-machine ~@body ::end)]
     (state-machine->lazy-seq state#)))

(rcf/tests
 (generator (yield :a)
            (yield :b)
            (yield :c))
 := '(:a :b :c))

(defn my-repeat [x]
  (generator
   (loop []
     (yield x)
     (recur))))

(rcf/tests
 (take 3 (my-repeat 'ho))
 := '(ho ho ho))

(defn my-iterate [f x]
  (generator
   (loop [x x]
     (yield x)
     (recur (f x)))))

(rcf/tests
 (take 10 (my-iterate (partial * 2) 1))
 := '(1 2 4 8 16 32 64 128 256 512))

(def fibonacci
  (generator
   (loop [prev 0 curr 1]
     (yield curr)
     (recur curr (+ prev curr)))))

(rcf/tests
 (take 10 fibonacci)
 := '(1 1 2 3 5 8 13 21 34 55))
