(ns fiddle.aero.core
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [hyperfiddle.rcf :refer [tests]]))

(def f (io/resource "fiddle/aero/config.edn"))
(def home (System/getenv "HOME"))

(defmethod aero/reader 'env!
  [opts _tag v]
  (let [v (if (instance? clojure.lang.Named v) (name v) v)]
    (or (System/getenv v)
        (get-in opts [:env v]))))

(tests
 (def r (aero/read-config f))

 (get r :a) := "a"
 (get r :home) := home

 ;; some operators
 (get r :or-v) := home
 (get r :join-v) := "hello world"
 (get r :merge-v) := {:a "a" :b "b"}

 ;; include
 (get-in r [:include0 :debug]) := true
 (get r :ref-include0-debug) := true

 ;; customized profile
 (get r :profile-v) := "default-v"
 (def r1 (aero/read-config f {:profile :dev}))
 (get r1 :profile-v) := "dev-v"

 ;; customized tag
 (get r1 :env!-home) := home
 (def r2 (aero/read-config f {:env {"ABC" "hello"}}))
 (System/getenv "ABC") := nil
 (get r2 :env!-abc) := "hello")
