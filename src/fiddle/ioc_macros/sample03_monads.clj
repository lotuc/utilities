(ns fiddle.ioc-macros.sample03-monads
  (:require
   [hyperfiddle.rcf :as rcf]
   [lotuc.clj.ioc-macros :as ioc]))

;;; https://github.com/leonoel/cloroutine/blob/master/doc/05-monads.md

(def BIND-IDX ioc/USER-START-IDX)
(def num-user-params 1)

(defn >>= [_m]
  (assert nil "yield used not in (async ...) block"))

(defn >>=* [state blk m]
  (let [bind (ioc/aget-object state BIND-IDX)
        m' (-> (fn [v]
                 (ioc/aset-all! state ioc/STATE-IDX blk ioc/VALUE-IDX v)
                 (let [s (ioc/copy-state-machine state)]
                   (ioc/run-state-machine s)
                   (ioc/aget-object s ioc/VALUE-IDX)))
               (bind m))]
    (ioc/aset-all! state ioc/VALUE-IDX m')))

(defmacro state-machine [& body]
  (let [terminators {`>>= `>>=*}
        crossing-env (zipmap (keys &env) (repeatedly gensym))]
    `(let [~@(mapcat (fn [[l sym]] [sym `(^:once fn* [] ~l)]) crossing-env)]
       (~(ioc/state-machine `(do ~@body) num-user-params [crossing-env &env] terminators)))))

;;; (unit x)
;;; (bind f m)
(defmacro mdo [monad & body]
  `(let [[unit# bind#] ~monad
         state# (state-machine (unit# (do ~@body)))
         captured-bindings# (clojure.lang.Var/getThreadBindingFrame)]
     (ioc/aset-all! state# ~ioc/BINDINGS-IDX captured-bindings#)
     (ioc/aset-object state# BIND-IDX bind#)
     (ioc/run-state-machine state#)
     (ioc/aget-object state# ioc/VALUE-IDX)))

(def seqable-monad [list mapcat])
(def nilable-monad [identity (fn [f x] (when (some? x) (f x)))])

(rcf/tests
 (mdo nilable-monad (+ (>>= 2) (>>= 3))) := 5
 (mdo nilable-monad (* (>>= nil) (>>= 2) (>>= 3))) := nil

 (mdo seqable-monad
      (* (>>= [1 2])
         (>>= [3 4])))
 := '(3 4 6 8))

(def state-monad
  [(fn [x] (fn [s] [x s]))
   (fn [f m] (fn [s] (let [[x s] (m s)] ((f x) s))))])

(defn state-eval [m s]
  (first (m s)))

(defn state-get [s]
  [s s])

(defn state-set [s]
  (fn [_] [nil s]))

(defn game [input]
  (mdo state-monad
       (let [[on score] (>>= state-get)]
         (if-some [[x & xs] (seq input)]
           (do (case x
                 \a (when on (>>= (state-set [on (inc score)])))
                 \b (when on (>>= (state-set [on (dec score)])))
                 \c (>>= (state-set [(not on) score])))
               (>>= (game xs))) score))))

(defn play [input]
  (state-eval (game input) [false 0]))

(rcf/tests
 (play "abcaaacbbcabbab") := 2)
