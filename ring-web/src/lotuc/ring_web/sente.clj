(ns lotuc.ring-web.sente
  (:require
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [taoensso.sente :as sente]))

(def adapter-packages
  {:http-kit "taoensso.sente.server-adapters.http-kit"
   :undertow "taoensso.sente.server-adapters.community.undertow"
   :immutant "taoensso.sente.server-adapters.community.immutant"
   :aleph "taoensso.sente.server-adapters.community.aleph"
   :nginx-clojure "taoensso.sente.server-adapters.community.nginx-clojure"})

(defn resolve-sch-adapter [package & [factory-fn]]
  (try (require [(symbol package)])
       (let [factory (resolve (symbol (str package "/" (or factory-fn "get-sch-adapter"))))]
         (factory))
       (catch Exception _
         (throw (ex-info (str "Failed to resolve get-sch-adapter: " package)
                         {:package package})))))

(defn- make-sch-adapter [{:keys [adapter get-sch-adapter] :as opts}]
  (cond
    (string? adapter)
    (resolve-sch-adapter adapter)

    (sequential? adapter)
    (apply resolve-sch-adapter adapter)

    (keyword? adapter)
    (if-let [p (get adapter-packages adapter)]
      (resolve-sch-adapter p)
      (throw (ex-info (str "Unknown adapter: " adapter) {:adapter adapter})))

    get-sch-adapter
    (do (assert (fn? get-sch-adapter) "get-sch-adapter must be 0-arity function that returns sch-adapter")
        (get-sch-adapter))

    :else
    (throw (ex-info "Either :adapter or :get-sch-adapter must be provided" {}))))

(defmethod ig/init-key :sente/chsk-server
  [k {:keys [adapter get-sch-adapter server-option]
      :as opts}]
  (log/infof "initialize %s%s" k (if adapter (str ": " adapter) ""))
  (let [sch-adapter (make-sch-adapter opts)
        server-option (merge {:packer :edn :csrf-token-fn nil} server-option)
        chsk-server (sente/make-channel-socket-server! sch-adapter server-option)]
    chsk-server))

(defmethod ig/halt-key! :sente/chsk-server
  [k {:keys [ch-recv] :as chsk-server}]
  ;; https://github.com/taoensso/sente/blob/8c73be5db24f260a01cc4159159997342cb414ff/wiki/3-FAQ.md?plain=1#L92
  (log/infof "halt %s" k)
  (when ch-recv
    (async/close! ch-recv)))
