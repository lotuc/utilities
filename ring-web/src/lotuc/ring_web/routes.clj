(ns lotuc.ring-web.routes
  (:require
   [clojure.string :as str]
   [integrant.core :as ig]
   [lotuc.ring-web.middleware :as middleware]
   [malli.transform :as mt]
   [reitit.coercion.malli :as rcm]
   [reitit.openapi :as openapi]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [ring.util.http-response :as http-response])
  (:import
   [java.util Date]))

(set! *warn-on-reflection* true)

(defn- singleton->vector [x]
  ;; allow comma separated parameters
  (if (string? x)
    (if (vector? x) x (str/split x #","))
    x))

(def ^:private custom-string-transformer
  (mt/transformer
   {:name :string
    :decoders (assoc (mt/-string-decoders) :vector singleton->vector)
    :encoders mt/-string-decoders}))

;;; https://github.com/metosin/reitit/issues/298
;;; - handling item lists in query params
(def ^:private custom-malli-coercion
  (rcm/create (assoc-in rcm/default-options
                        [:transformers :string :default]
                        custom-string-transformer)))

(def api-middlewares
  [;; query-params & form-params
   parameters/parameters-middleware
   ;; content-negotiation
   muuntaja/format-negotiate-middleware
   ;; encoding response body
   muuntaja/format-response-middleware
   ;; exception handling
   coercion/coerce-exceptions-middleware
   ;; decoding request body
   muuntaja/format-request-middleware
   ;; coercing response bodys
   coercion/coerce-response-middleware
   ;; coercing request parameters
   coercion/coerce-request-middleware
   ;; exception handling
   middleware/wrap-exception])

(defn route-data [{:keys [route-data]}]
  (cond-> {:coercion custom-malli-coercion
           :muuntaja middleware/instance
           :middleware api-middlewares}
    route-data (merge route-data)))

(defn healthcheck! []
  {:handler (fn [_]
              (http-response/ok
               {:time (Date. (System/currentTimeMillis))
                :up-since (Date. (.getStartTime (java.lang.management.ManagementFactory/getRuntimeMXBean)))
                :app {:status "up" :message ""}}))
   :responses {:200 {:body {:time inst? :up-sicne inst? :app {:status string? :message string?}}}}})

;; Routes
(defn api-routes [{:keys [routes built-in-routes]}]
  [(when-some [swagger (:swagger built-in-routes)]
     ["/swagger.json"
      {:get {:no-doc  true
             :swagger (dissoc swagger :middleware)
             :middleware (:middleware swagger)
             :handler (swagger/create-swagger-handler)}}])
   (when-some [openapi (:openapi built-in-routes)]
     ["/openapi.json"
      {:get {:no-doc true
             :openapi (dissoc openapi :middleware)
             :middleware (:middleware openapi)
             :handler (openapi/create-openapi-handler)}}])
   (when (:health built-in-routes)
     ["/health" {:get (healthcheck!)}])
   (mapv (fn [route] (if (fn? route) (route) route)) routes)])

(derive :reitit.routes/api :reitit/routes)

(defmethod ig/init-key :reitit.routes/api
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (fn [] [base-path (route-data opts) (api-routes opts)]))
