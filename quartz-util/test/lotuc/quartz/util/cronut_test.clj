(ns lotuc.quartz.util.cronut-test
  (:require
   [clojure.test :refer [deftest is]]
   [lotuc.quartz.util.cronut :as cronut])
  (:import
   [java.util TimeZone]))

;; MIT License
;;
;; Copyright (c) 2022 Factor House
;;
;; Permission is hereby granted, free of charge, to any person obtaining a copy
;; of this software and associated documentation files (the "Software"), to deal
;; in the Software without restriction, including without limitation the rights
;; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
;; copies of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:
;;
;; The above copyright notice and this permission notice shall be included in all
;; copies or substantial portions of the Software.
;;
;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
;; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
;; SOFTWARE.

;;; The original source comes from: https://github.com/factorhouse/cronut

(deftest base-trigger

  (is (= {:group       "DEFAULT"
          :description nil
          :priority    5}
         (-> (cronut/base-trigger-builder {})
             (.build)
             (bean)
             (select-keys [:group :description :priority]))))

  (is (= {:group       "test"
          :name        "trigger-two"
          :description "test trigger"
          :priority    101
          :startTime   #inst "2019-01-01T00:00:00.000-00:00"
          :endTime     #inst "2019-02-01T00:00:00.000-00:00"}
         (-> (cronut/base-trigger-builder
              {:key    ["test" "trigger-two"]
               :description "test trigger"
               :start       #inst "2019-01-01T00:00:00.000-00:00"
               :end         #inst "2019-02-01T00:00:00.000-00:00"
               :priority    101})
             (.build)
             (bean)
             (select-keys [:group :name :description :startTime :endTime :priority])))))

(deftest simple-schedule

  (is (= {:repeatCount        0
          :repeatInterval     0
          :misfireInstruction 0}
         (-> (cronut/simple-schedule nil)
             (.build)
             (bean)
             (select-keys [:repeatInterval :repeatCount :misfireInstruction]))))

  (is (= {:repeatCount        0
          :repeatInterval     1000
          :misfireInstruction 0}
         (-> (cronut/simple-schedule {:interval 1000})
             (.build)
             (bean)
             (select-keys [:repeatInterval :repeatCount :misfireInstruction]))))

  (is (= {:repeatCount        -1
          :repeatInterval     1000
          :misfireInstruction 0}
         (-> (cronut/simple-schedule {:interval 1000
                                      :repeat   :forever})
             (.build)
             (bean)
             (select-keys [:repeatInterval :repeatCount :misfireInstruction]))))

  (is (= {:repeatCount        10
          :repeatInterval     1000
          :misfireInstruction 0}
         (-> (cronut/simple-schedule {:interval 1000
                                      :repeat   10})
             (.build)
             (bean)
             (select-keys [:repeatInterval :repeatCount :misfireInstruction]))))

  (is (= {:repeatCount        10
          :repeatInterval     1000000
          :misfireInstruction 0}
         (-> (cronut/simple-schedule {:interval  1000
                                      :repeat    10
                                      :time-unit :seconds})
             (.build)
             (bean)
             (select-keys [:repeatInterval :repeatCount :misfireInstruction]))))

  (is (= {:repeatCount        10
          :repeatInterval     1000000
          :misfireInstruction 5}
         (-> (cronut/simple-schedule {:interval  1000
                                      :repeat    10
                                      :time-unit :seconds
                                      :misfire   :next-existing})
             (.build)
             (bean)
             (select-keys [:repeatInterval :repeatCount :misfireInstruction])))))

(deftest cron-schedule

  (is (thrown? IllegalArgumentException
               (cronut/cron-schedule {})))

  (is (= {:cronExpression     "*/6 * * * * ?"
          :timeZone           (TimeZone/getDefault)
          :misfireInstruction 0}
         (-> (cronut/cron-schedule {:cron "*/6 * * * * ?"})
             (.build)
             (bean)
             (select-keys [:cronExpression :timeZone :misfireInstruction]))))

  (is (= {:cronExpression     "*/6 * * * * ?"
          :timeZone           (TimeZone/getTimeZone "UTC")
          :misfireInstruction 0}
         (-> (cronut/cron-schedule {:cron      "*/6 * * * * ?"
                                    :time-zone "UTC"})
             (.build)
             (bean)
             (select-keys [:cronExpression :timeZone :misfireInstruction]))))

  (is (= {:cronExpression     "*/6 * * * * ?"
          :timeZone           (TimeZone/getTimeZone "UTC")
          :misfireInstruction 1}
         (-> (cronut/cron-schedule {:cron      "*/6 * * * * ?"
                                    :time-zone "UTC"
                                    :misfire   :fire-and-proceed})
             (.build)
             (bean)
             (select-keys [:cronExpression :timeZone :misfireInstruction])))))
