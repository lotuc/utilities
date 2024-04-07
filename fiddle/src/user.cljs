(ns user
  (:require
   [fiddle.ring-web.sample4-sente-websocket.client]
   [hyperfiddle.rcf :refer [tests]]))

;;; runtime enable
(hyperfiddle.rcf/enable!)

;;;
(tests
 "rcf tests works"
 42 := 42)
