(ns lotuc.ring-web.native-app
  (:require
   [clojure.java.io :as io]
   [kit.edge.server.http-kit]
   [lotuc.ring-web.core :as core]
   [lotuc.ring-web.sente-server]
   [ring.util.response :as response]
   [lotuc.ring-web.quartz-tasks])
  (:gen-class))

;;; handling swagger static resources.
(defmethod response/resource-data :resource [u]
  (with-open [r (.openStream ^java.net.URL u)]
    (let [content (slurp r)]
      {:content content
       :content-length (count content)})))

(defn -main [& _args]
  (core/start-app (io/resource "native_app.edn") {:profile :prod}))
