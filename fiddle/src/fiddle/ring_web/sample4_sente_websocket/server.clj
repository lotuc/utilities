(ns fiddle.ring-web.sample4-sente-websocket.server
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [integrant.repl]
   [kit.edge.server.undertow]
   [lotuc.ring-web.core :refer [system-config]]
   [lotuc.ring-web.sente]
   [ring.middleware.cors :as cors :refer [wrap-cors]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.util.response :as response]
   [taoensso.sente :as sente]))

(defn- wrap-base [handler {:keys [cors-configuration]}]
  ;; turn off cors for dev purpose
  (-> handler
      (wrap-cors
       :access-control-allow-origin [#".*"]
       :access-control-allow-headers
       ["Access-Control-Allow-Origin" "Origin" "X-Requested-With" "Content-Type" "Accept" "Authorization" "x-csrf-token" "x-requested-with"]
       :access-control-allow-methods #{:get :post :put :delete :options}
       :access-control-allow-credentials ["true"])))

(defn- handle-sente-message
  [{:keys [event id ?data ?reply-fn
           send-fn uid client-id ring-req
           ch-recv connected-uids]
    :as msg}]
  (case id
    :chsk/ws-ping (prn :<sente-msg [uid client-id] "ws-ping from client")
    :chsk/ws-pong (prn :<sente-msg [uid client-id] "ws-pong from client")
    :chsk/uidport-open (prn :<sente-msg [uid client-id] "uidport-open:" ?data)
    :chsk/uidport-close (prn :<sente-msg [uid client-id] "uidport-close:" ?data)
    :chsk/bad-package (prn :<sente-msg [uid client-id] "bad-package:" ?data)
    :chsk/bad-event (prn :<sente-msg [uid client-id] "bad-package:" ?data)

    :a/ping (?reply-fn [:a/pong ?data])
    :a/handshake (?reply-fn [:a/handshake {:message "Hello, World!"}])
    (prn :<sente-msg "!!!" [uid client-id] event)))

(defmethod aero/reader 'sample3/middleware [_aero-opts _tag _value] wrap-base)
(defmethod aero/reader 'sample3/senete-msg-handler [_aero-opts _tag _value] #'handle-sente-message)

(defmethod aero/reader 'sample3/sente-server-option
  [_ _ v]
  ;; when authorized, return `chsk/handshake` with
  ;; [ user-id (extract from `user-id-fn`)
  ;;   nil <compatible>
  ;;   ?handshake-data (extracted from `handshake-data-fn`)
  ;;   ?first-handshake
  ;; ]
  (merge {:authorized?-fn
          (fn [{:keys [websocket? params headers cookies] :as ring-req}]
            (prn :>>>authorized?-fn
                 {:websocket? websocket?
                  ;; this is only available when requesting comes from ajax (set
                  ;; by `:ajax-opts`)
                  :headers (get headers "authorization")
                  ;; this is set by `:params`
                  :params (:authorization params)
                  ;; and cookies can be used
                  :cookie (get-in cookies ["authorization" :value])})
            true)

          :user-id-fn
          (fn [{:keys [websocket? params headers cookies] :as ring-req}]
            ;; extract user id from request (like authorized?-fn, you may choose
            ;; the datasource from params, headers or cookies)
            (let [user-id (get-in cookies ["user-id" :value])]
              (prn :>>>user-id-fn user-id)
              user-id))

          :handshake-data-fn
          (fn [{:keys [websocket? session cookies] :as ring-req}]
            (let [user-id (get-in cookies ["user-id" :value])]
              (prn :>>>handshake-data-fn user-id)
              {:shake "hands" :username "Lotuc"}))}

         v))

(defn- setup-session
  [{:keys [parameters session cookies]}]
  (-> (response/response "{}")
      (response/content-type "application/json")
      (response/set-cookie "user-id" "42")
      (response/set-cookie "authorization" "42")))

(derive :sente/chsk-route :reitit.routes.api/routes)

(defmethod ig/init-key :sente/chsk-route
  [_ {:keys [base-path chsk-server]}]
  (log/infof "initialize :sente/route")
  (let [{:keys [ajax-post-fn ajax-get-or-ws-handshake-fn]} chsk-server]
    [(or base-path "") {:middleware [[wrap-session {:cookie-name "sente-session"}]]}
     ["/setup-session" {:post {:handler #'setup-session}}]
     ["/chsk" {:get ajax-get-or-ws-handshake-fn
               :post ajax-post-fn}]]))

(defmethod ig/init-key :sente/chsk-message-handler
  [_ {{:keys [ch-recv]} :chsk-server}]
  (when ch-recv
    (sente/start-chsk-router! ch-recv #'handle-sente-message)))

(defmethod ig/halt-key! :sente/chsk-message-handler
  [_ stop-chsk-router]
  (when stop-chsk-router
    (stop-chsk-router)))

;;; requires `kit.edge.server.undertow` whose config key is `:server/http`, it
;;; uses handler build from `:handler/ring`.

(comment
  ;; setup the configuration
  (integrant.repl/set-prep!
   #(system-config (io/resource "fiddle/ring_web/sample4_sente_websocket/system.edn")
                   {:profile :dev}))

  (require '[user])
  (user/system :sente/chsk-server)
  ;; start/restart the system
  (integrant.repl/go))
