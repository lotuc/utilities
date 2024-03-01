(ns fiddle.ring-web.sample4-sente-websocket.client
  (:require
   [missionary.core :as m]
   [taoensso.sente :as sente]))

(def sente-conn (atom nil))

(defn chsk-send!
  [{:keys [send-fn]} event & [?timeout-ms]]
  (fn [ok _]
    (send-fn event ?timeout-ms ok)
    (fn [])))

(defn event-msg-handler
  [{:keys [event id ?data send-fn
           state ch-recv]
    :as msg}]
  (case id
    :chsk/ws-ping   (js/console.info :<sente-msg "ws-ping from server")
    :chsk/handshake (js/console.info :<sente-msg "handshake from server" ?data)
    :chsk/state     (js/console.info :<sente-msg "chsk/state" ?data)
    :chsk/recv      (js/console.info :<sente-msg "chsk/recv" ?data)
    (js/console.warn :<sente-msg "!!" id ?data)))

(defn ajax-call
  [endpoint {:keys [xhr-cb-fn] :as options}]
  (fn [ok _]
    (let [!xhr (atom nil)
          options (->> (fn [?xhr]
                         (when (= :abort (first (reset-vals! !xhr ?xhr)))
                           (when ?xhr (.abort ?xhr)))
                         (when xhr-cb-fn (xhr-cb-fn ?xhr)))
                       (assoc options :xhr-cb-fn))]
      (sente/ajax-lite endpoint options (fn [resp-map] (ok resp-map)))
      (fn []
        (when-some [xhr (first (reset-vals! !xhr :abort))]
          (.abort xhr))))))

(def setup-session!
  (->> {:method :post
        :resp-type :auto
        :params {}
        :timeout-ms 7000
        :xhr-timeout-ms 2500}
       (ajax-call "http://localhost:4242/api/setup-session")))

(def setup-sente-conn
  (m/sp
   (js/console.info :setup-session (m/? setup-session!))
   (let [conn (->> {:type :auto
                    :packer :edn :port 4242
                    :params {:authorization "42"}
                    :ajax-opts {:headers {"Authorization" "42"}}}
                   (sente/make-channel-socket-client! "/api/chsk" nil))]
     (when-some [ch-recv (:ch-recv conn)]
       (sente/start-chsk-router! ch-recv event-msg-handler))
     (reset! sente-conn conn))))

(js/Promise. setup-sente-conn)

(comment
  (js/Promise.
   (m/sp
    (let [evt [:a/ping 42]]
      (js/console.info :>send evt)
      (js/console.info :<recv (m/? (chsk-send! @sente-conn evt 1000))))))

  @(:state @sente-conn)

  ;;
  )
