(ns user
  (:require
   [hyperfiddle.rcf :refer [tests]]))

;;; runtime enable
(hyperfiddle.rcf/enable!)

;;;
(tests
 "rcf tests works"
 42 := 42)
