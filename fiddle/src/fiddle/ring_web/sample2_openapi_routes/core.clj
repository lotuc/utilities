(ns fiddle.ring-web.sample2-openapi-routes.core
  (:require
   [clojure.java.io :as io]
   [integrant.core :as ig]
   [integrant.repl]
   [kit.edge.server.undertow]
   [lotuc.ring-web.core :refer [system-config]]
   [ring.util.http-response :as http-response]))

(defn- get-math-add [{:keys [parameters session cookies]}]
  (let [{:keys [x y]} (:query parameters)]
    (http-response/ok {:total (+ x y)})))

(defn- post-math-add [{:keys [parameters]}]
  (let [{:keys [x y]} (:body parameters)]
    (http-response/ok {:total (+ x y)})))

(defn- get-math-add-path [{:keys [parameters]}]
  (let [{:keys [x y]} (:path parameters)]
    (http-response/ok {:total (+ x y)})))

(defn- apply-math-op [{:keys [op int-args]}]
  (apply (case op :add + :sub - :mul * :div /) int-args))

(defn- get-math-op [{:keys [parameters]}]
  (http-response/ok {:res (apply-math-op (:query parameters))}))

(defn- post-math-op [{:keys [parameters]}]
  (http-response/ok {:res (apply-math-op (:body parameters))}))

;;; why this?
;;;
;;; search for `:reitit.routes.api/routes` in `ring-web-common.edn` for details.
(derive :reitit.routes.api/math :reitit.routes.api/routes)

(defmethod ig/init-key :reitit.routes.api/math
  [_ {:keys [base-path]}]
  [(or base-path "")
   ["/math"
    ["/add"
     ["" {:get {:parameters {:query [:map [:x int?] [:y int?]]}
                :responses {200 {:body [:map [:total int?]]}}
                :handler #'get-math-add}
          :post {:parameters {:body [:map [:x int?] [:y int?]]}
                 :responses {200 {:body [:map [:total int?]]}}
                 :handler #'post-math-add}}]
     ["/:x/:y" {:get {:parameters {:path [:map [:x int?] [:y int?]]}
                      :responses {200 {:body [:map [:total int?]]}}
                      :handler #'get-math-add-path}
                ;; malli lite syntax (https://github.com/metosin/malli?tab=readme-ov-file#lite)
                :post {:parameters {:path {:x int? :y int?}}
                       :responses {200 {:body [:map [:total int?]]}}
                       :handler #'get-math-add-path}}]]
    ["" {:get {:parameters {:query [:map [:op keyword?]
                                    [:int-args [:vector int?]]]}
               :handler #'get-math-op}
         :post {:parameters {:body [:map [:op keyword?]
                                    [:int-args [:vector int?]]]}
                :handler #'post-math-op}}]]])

(comment
  (integrant.repl/set-prep!
   #(system-config (io/resource "fiddle/ring_web/sample2_openapi_routes/system.edn")
                   {:profile :dev}))

  ;; start/restart the system
  (integrant.repl/go)

  ;; curl http://localhost:4242/api/math/add\?x\=4\&y\=2
  ;; curl -X POST -H 'Content-Type: application/json' http://localhost:4242/api/math/add -d '{"x":4,"y":2}'
  ;; curl http://localhost:4242/api/math/add/4/2

  ;; curl http://localhost:4242/api/math\?op\=add\&int-args\=1,2
  ;; curl -X POST -H 'Content-Type: application/json' http://localhost:4242/api/math -d '{"op":"add","int-args":[1,2]}'
  )
