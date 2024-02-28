(ns lotuc.ring-web.middleware
  (:require
   [clojure.tools.logging :as log]
   [reitit.ring.middleware.exception :as exception]
   [luminus-transit.time :as time]
   [muuntaja.core :as muuntaja]
   [ring.middleware.defaults :as defaults]
   [ring.middleware.session.cookie :as cookie]))

(defn handler [message status exception request]
  (when (>= status 500)
    ;; You can optionally use this to report error to an external service
    (log/error exception))
  {:status status
   :body   {:message   message
            :exception (.getClass exception)
            :data      (ex-data exception)
            :uri       (:uri request)}})

(def wrap-exception
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {:system.exception/internal     (partial handler "internal exception" 500)
     :system.exception/business     (partial handler "bad request" 400)
     :system.exception/not-found    (partial handler "not found" 404)
     :system.exception/unauthorized (partial handler "unauthorized" 401)
     :system.exception/forbidden    (partial handler "forbidden" 403)

     ;; override the default handler
     :system.exception/default      (partial handler "default" 500)

     ;; print stack-traces for all exceptions
     :system.exception/wrap         (fn [handler e request]
                                      (handler e request))})))

(def instance
  (muuntaja/create
   (-> muuntaja/default-options
       (update-in
        [:formats "application/transit+json" :decoder-opts]
        (partial merge time/time-deserialization-handlers))
       (update-in
        [:formats "application/transit+json" :encoder-opts]
        (partial merge time/time-serialization-handlers)))))

(defn wrap-base
  [{:keys [metrics site-defaults-config cookie-secret middleware] :as opts}]
  (let [cookie-store (cookie/cookie-store {:key (.getBytes ^String cookie-secret)})
        defaults-config (assoc-in site-defaults-config [:session :store] cookie-store)
        middleware (or middleware (fn [handler _] handler))]
    (fn [handler]
      (cond-> handler
        middleware (middleware opts)
        true (defaults/wrap-defaults defaults-config)))))
