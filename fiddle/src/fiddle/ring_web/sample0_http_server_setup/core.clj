(ns fiddle.ring-web.sample0-http-server-setup.core
  (:require
   [clojure.java.io :as io]
   [integrant.repl]
   [kit.edge.server.undertow]
   [lotuc.ring-web.core :refer [system-config]]))

;;; requires `kit.edge.server.undertow` whose config key is `:server/http`
;;; - it builds up a undertow web server
;;; - which uses ring handler build from `:handler/ring`.

(comment
  ;; setup the configuration
  (integrant.repl/set-prep!
   #(system-config (io/resource "fiddle/ring_web/sample0_http_server_setup/system.edn")
                   {:profile :dev}))

  ;; start/restart the system
  (integrant.repl/go)

  ;; Swagger doc
  ;; - http://localhost:4242/api-docs/index.html

  ;; Checkout this API
  ;; - curl http://localhost:4242/api/health
  )
