(ns fiddle.ring-web.sample1-customize-handler-middleware.core
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [integrant.repl]
   [kit.edge.server.undertow]
   [lotuc.ring-web.core :refer [system-config]]))

;;; Enabled two middlewares
;;; - [`:handler/ring` `:middleware`]
;;; - [`:reitit.routes/api` `:built-in-routes` `openapi` `middleware`]

(def customized-middleware
  (fn middleware [h options]
    (log/infof ":options %s" options)
    (fn [req]
      (log/infof "customized middleware works (req keys: %s)" (keys req))
      (h req))))

(defmethod aero/reader 'sample1/middleware
  [{:keys [profile] :as aero-opts} _tag value]
  (log/infof "building middleware for %s with %s" profile value)
  customized-middleware)

(comment
  ;; customized handler middleware with customized midddleware reader
  (integrant.repl/set-prep!
   #(system-config (io/resource "fiddle/ring_web/sample1_customize_handler_middleware/system.edn")
                   {:profile :dev}))

  ;; start/restart the system
  (integrant.repl/go)

  ;; check if the middleware works
  ;;   - curl http://localhost:4242/api/health
  ;;
  ;; open the API doc in browser
  ;;   - http://localhost:4242/api-docs/index.html
  )
