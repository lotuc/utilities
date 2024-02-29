(ns lotuc.ring-web.handler
  (:require
   [integrant.core :as ig]
   [lotuc.ring-web.middleware :as middleware]
   [reitit.ring :as ring]
   [reitit.swagger-ui :as swagger-ui]
   [ring.util.response :as response]))

(set! *warn-on-reflection* true)

(defmethod ig/init-key :handler/ring
  [_ {:keys [router swagger-ui] :as opts}]
  (ring/ring-handler
   (router)
   (ring/routes
    ;; Handle trailing slash in routes - add it + redirect to it
    ;; https://github.com/metosin/reitit/blob/master/doc/ring/slash_handler.md
    (ring/redirect-trailing-slash-handler)
    (ring/create-resource-handler {:path "/"})
    (when swagger-ui
      ;; https://cljdoc.org/d/metosin/reitit/0.7.0-alpha7/doc/ring/swagger-support
      (swagger-ui/create-swagger-ui-handler swagger-ui))
    (ring/create-default-handler
     {:not-found
      (constantly (-> {:status 404, :body "Page not found"}
                      (response/content-type "text/html")))
      :method-not-allowed
      (constantly (-> {:status 405, :body "Not allowed"}
                      (response/content-type "text/html")))
      :not-acceptable
      (constantly (-> {:status 406, :body "Not acceptable"}
                      (response/content-type "text/html")))}))
   {:middleware [(middleware/wrap-base opts)]}))

(defmethod ig/init-key :router/routes
  [_ {:keys [routes]}]
  (mapv (fn [route] (if (fn? route) (route) route)) routes))

(defmethod ig/init-key :router/core
  [_ {:keys [routes env] :as opts}]
  (if (= env :dev)
    #(ring/router ["" opts routes])
    (constantly (ring/router ["" opts routes]))))
