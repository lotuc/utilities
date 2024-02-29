(ns fiddle.ring-web.sample0
  (:require
   [clojure.java.io :as io]
   [integrant.repl]
   [kit.edge.server.undertow]
   [lotuc.ring-web.core :refer [system-config]]))

;;; requires `kit.edge.server.undertow` whose config key is `:server/http`, it
;;; uses handler build from `:handler/ring`.

(comment
  ;; setup the configuration
  (integrant.repl/set-prep!
   #(system-config (io/resource "fiddle/ring_web/sample0.edn") {:profile :dev}))

  ;; start/restart the system
  (integrant.repl/go)

  ;; Swagger doc
  ;; - http://localhost:4242/api-doc/index.html

  ;; Checkout this API
  ;; - curl http://localhost:4242/api/health
  )
