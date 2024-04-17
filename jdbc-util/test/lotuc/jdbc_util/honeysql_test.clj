(ns lotuc.jdbc-util.honeysql-test
  (:require
   [clojure.test :refer :all]
   [honey.sql.helpers :as h]
   [lotuc.jdbc-util.honeysql :as h1]))

(deftest join-by-deduplicated
  (is (= {:join-by [:left [:a :condition1 :b :condition2]
                    :right [:b :condition]]}
         (-> {}
             (h/join-by :left [:a :condition1]
                        :right [:b :condition])
             (h1/join-by :left [:b :condition2])))))

(def join-fns
  {:join h1/join
   :left-join h1/left-join
   :right-join h1/right-join
   :inner-join h1/inner-join
   :outer-join h1/outer-join
   :full-join h1/full-join
   :cross-join h1/cross-join})

(deftest join-deduplicated
  (is (= {:join [:a :condition3 :b :condition2 :c :condition4]}
         (-> {}
             (h1/join :a :condition1 :b :condition2)
             (h1/join :a :condition3 :c :condition4))))

  (testing "more join functions"
    (doseq [[k f] join-fns]
      (is (= {k [:a :condition3 :b :condition2 :c :condition4]}
             (-> {}
                 (f :a :condition1 :b :condition2)
                 (f :a :condition3 :c :condition4)))))))
