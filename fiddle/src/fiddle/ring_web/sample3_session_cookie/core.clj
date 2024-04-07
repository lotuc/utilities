(ns fiddle.ring-web.sample3-session-cookie.core
  (:require
   [clojure.java.io :as io]
   [integrant.core :as ig]
   [integrant.repl]
   [kit.edge.server.undertow]
   [lotuc.ring-web.core :refer [system-config]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.util.response :as response]))

(derive :reitit.routes.api/hello :reitit.routes.api/routes)

(defn- return-and-update-session [{:keys [session]}]
  (let [session-n (session :n 1)]
    (-> (response/response (str "session var n = " session-n "\n"))
        (response/content-type "text/plain")
        (assoc-in [:session :n] (inc session-n)))))

(defn- return-and-update-cookie [{:keys [cookies]}]
  (let [cookie-n (parse-long (or (get-in cookies ["n" :value]) "1"))]
    (-> (response/response (str "cookie var n = " cookie-n))
        (response/content-type "text/plain")
        (response/set-cookie :n (inc cookie-n)))))

(defmethod ig/init-key :reitit.routes.api/hello
  [_ {:keys [base-path]}]
  [(or base-path "")
   ;; this is enabled by `:site-defaults-config`
   ["/session/default" {:get {:handler #'return-and-update-session}}]
   ;; creates a session with cookie-name "session42"
   ["/session/customized" {:middleware [[wrap-session {:cookie-name "session42"}]]
                           :get {:handler #'return-and-update-session}}]
   ;; this is enabled by `:site-defaults-config`
   ["/cookie" {:get {:handler #'return-and-update-cookie}}]])

(comment
  ;; customized handler middleware with customized midddleware reader
  (integrant.repl/set-prep!
   #(system-config (io/resource "fiddle/ring_web/sample3_session_cookie/system.edn")
                   {:profile :dev}))

  ;; start/restart the system
  (integrant.repl/go)

  ;;
  )
