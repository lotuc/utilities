(ns fiddle.ring-web.sample1
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [integrant.repl]
   [kit.edge.server.undertow]
   [lotuc.ring-web.core :refer [system-config]]
   [ring.util.http-response :as http-response]))

(defmethod aero/reader 'ring/middleware
  [aero-opts _tag value]
  (if-some [m (get aero-opts :ring/middleware)]
    (fn [h opts1] (m h (merge opts1 value)))
    (do (log/warnf "ring/middleware not found: %s" aero-opts)
        (throw (ex-info (str "ring/middleware not found")
                        {:aero-opts aero-opts})))))

(defn middleware
  "Checkout sample1.edn `:handler/ring`/`:middleware` for how this is
  configured."
  [h {:keys [customized-option site-defaults-config]}]
  (log/infof ":customized-option %s - site-defaults-config %s"
             customized-option site-defaults-config)
  h)

;;; custom api routes

(derive :reitit.routes.api/hello :reitit.routes.api/routes)

(defmethod ig/init-key :reitit.routes.api/hello
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  [base-path
   ["/hello" {:get (fn [_] (http-response/ok {:message "Hello, World!"}))}]])

(comment
  ;; customized handler middleware with customized midddleware reader
  (integrant.repl/set-prep!
   #(system-config (io/resource "fiddle/ring_web/sample1.edn")
                   {:profile :dev :ring/middleware middleware}))

  ;; start/restart the system
  (integrant.repl/go)

  ;; Checkout this API
  ;; - curl http://localhost:4242/api/hello
  )
