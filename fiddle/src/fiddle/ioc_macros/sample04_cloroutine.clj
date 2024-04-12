(ns fiddle.ioc-macros.sample04-cloroutine
  (:require
   [hyperfiddle.rcf :as rcf]
   [lotuc.clj.ioc-macros :as ioc]))

(def RESUME-IDX ioc/USER-START-IDX)
(def num-user-params 1)

(defmacro break-on [break-resume state blk v]
  `(ioc/aset-all! ~state ioc/STATE-IDX ~blk ioc/VALUE-IDX ~v ~RESUME-IDX ~break-resume))

(defn break-terminal
  ([break-suspend break-resume state blk a] (break-on break-resume state blk (break-suspend a)))
  ([break-suspend break-resume state blk a b] (break-on break-resume state blk (break-suspend a b)))
  ([break-suspend break-resume state blk a b c] (break-on break-resume state blk (break-suspend a b c)))
  ([break-suspend break-resume state blk a b c & ds] (break-on break-resume state blk (apply break-suspend a b c ds))))

(defmacro state-machine [terminators & body]
  (let [crossing-env (zipmap (keys &env) (repeatedly gensym))]
    `(let [~@(mapcat (fn [[l sym]] [sym `(^:once fn* [] ~l)]) crossing-env)]
       (~(ioc/state-machine `(do ~@body) num-user-params [crossing-env &env] terminators)))))

(defmacro cr* [terminators state]
  `(let [captured-bindings# (clojure.lang.Var/getThreadBindingFrame)]
     (ioc/aset-all! ~state ~ioc/BINDINGS-IDX captured-bindings#)
     (when-some [r# (ioc/aget-object ~state RESUME-IDX)]
       (ioc/aset-all! ~state RESUME-IDX nil ioc/VALUE-IDX (r#)))
     (with-redefs [~@(mapcat (fn [[k v]] [v `(partial break-terminal ~k ~v)])
                             terminators)]
       (ioc/run-state-machine ~state))
     (ioc/aget-object ~state ioc/VALUE-IDX)))

(defmacro cloroutine [terminators]
  `(fn cloroutine# [state#]
     (fn
       ([] (cr* ~terminators state#))
       ([f#] (f# (cloroutine# (ioc/copy-state-machine state#))))
       ([f# a#] (f# (cloroutine# ~terminators (ioc/copy-state-machine state#)) a#))
       ([f# a# b#] (f# (cloroutine# ~terminators (ioc/copy-state-machine state#)) a# b#))
       ([f# a# b# c#] (f# (cloroutine# ~terminators (ioc/copy-state-machine state#)) a# b# c#))
       ([f# a# b# c# & ds#] (apply f# (cloroutine# ~terminators (ioc/copy-state-machine state#)) a# b# c# ds#)))))

;;; https://github.com/leonoel/cloroutine/blob/master/src/cloroutine.core.cljc
(defmacro cr [breaks & body]
  `(let [s# (state-machine ~breaks ~@body)]
     ((cloroutine ~breaks) s#)))

;;; cr interface testing

(defn pause [v] v)
(defn resume [] 1)

;;; TODO: the break's namespace need to be properly handled
(def r0 (cr {fiddle.ioc-macros.sample04-cloroutine/pause fiddle.ioc-macros.sample04-cloroutine/resume}
            (+ (pause 4) (pause 2) (pause 42))))
;;; stops at (pause 4) and returns 4
(rcf/tests
 (r0) := 4)
;;; copy the paused coroutine
(def r1 (r0 identity))
;;; will pause at (pause 2) and returns 2
(rcf/tests
 (r0) := 2
 (r1) := 2
 (r0) := 42
 (r1) := 42
 (r0) := 3
 (r1) := 3)

;;; the generators sample

(def ^:dynamic *tail* nil)

(defn gen-seq [gen]
  (lazy-seq (binding [*tail* (gen-seq gen)] (gen))))

(defn yield [x]
  (cons x *tail*))

(defn no-op [])

(defmacro generator [& body]
  `(gen-seq (cr {yield no-op} ~@body nil)))

(rcf/tests
 (generator
  (yield :a)
  (yield :b)
  (yield :c))
 := '(:a :b :c))
