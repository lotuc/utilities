(ns lotuc.ring-web.config
  "Configuration related.

  [kit.config](https://github.com/kit-clj/kit/blob/master/libs/kit-core/src/kit/config.clj)
  provides
  1. aero readers for 'ig/ref 'ig/refset (delegates to integrant)
  2. integrant system key :system/env"
  (:require
   [aero.core :as aero]
   [hyperfiddle.rcf :refer [tests]]
   [kit.config]
   [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]))

(set! *warn-on-reflection* true)

(defmethod aero/reader 'env!
  [opts _tag v]
  (let [v (if (instance? clojure.lang.Named v) (name v) v)]
    (or (System/getenv v) (get-in opts [:env v]))))

(defmethod aero/reader 'middleware/basic-authentication
  [_opts _tag {:keys [authenticated? user->pass]}]
  #(wrap-basic-authentication
    % (fn [user pass]
        (and user->pass
             (contains? user->pass user)
             (= (get user->pass user) pass)))))

(defn deep-merge [a b]
  (if (map? a)
    (merge-with deep-merge a b)
    b))

(tests
 (deep-merge {:a 1 :b 2} {:b 3 :c 4}) :=  {:a 1 :b 3 :c 4}
 (deep-merge {:p {:a 1 :b 2} :p0 2} {:p {:b 3 :c 4} :p1 4}) := {:p {:a 1 :b 3 :c 4} :p0 2 :p1 4})
