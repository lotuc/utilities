(ns lotuc.ring-web.core-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [kit.edge.server.undertow]
   [lotuc.ring-web.core :as core]))

(use-fixtures
  :once (core/system-fixture
         (io/resource "lotuc/ring_web/system1.edn")
         {:env {"PORT" (+ 3000 (rand-int 10000))}}))

(deftest http-server-test
  (testing "Start a http server with undertow"
    (is (some? (:server/http (core/system-state))))
    (is (= 1 1))))
