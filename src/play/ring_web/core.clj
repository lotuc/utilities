(ns play.ring-web.core
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [integrant.repl]
   [kit.edge.server.undertow]
   [lotuc.ring-web.core]
   [ring.util.http-response :as http-response]))

(defmethod aero/reader 'ring/middleware
  [aero-opts _tag value]
  (if-some [m (get aero-opts :ring/middleware)]
    (fn [h opts1] (m h (merge opts1 value)))
    (do (log/warnf "ring/middleware not found: %s" aero-opts)
        (throw (ex-info (str "ring/middleware not found")
                        {:aero-opts aero-opts})))))

(def dev-config
  {:profile :dev
   :ring/middleware (fn [h opts]
                      (log/infof ":customized-option %s - site-defaults-config %s"
                                 (:customized-option opts) (:site-defaults-config opts))
                      h)})

(integrant.repl/set-prep!
 #(aero/read-config (io/resource "play/ring_web/config.edn") dev-config))

;;; custom api routes

(derive :reitit.routes.api/hello :reitit.routes.api/routes)

(defmethod ig/init-key :reitit.routes.api/hello
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  [base-path
   ["/hello" {:get (fn [_] (http-response/ok {:message "Hello, World!"}))}]])

(comment
  (integrant.repl/go))
