(ns lotuc.ring-web.core
  "generated by https://github.com/kit-clj & did some modification.

  `lotuc.ring-web.handler` provides
  - `:router/routes`: collect all routes of type `:reitit/routes`
  - `:router/core`: build router with routes
  - `:handler/ring`: build ring handler with router and other options

  `lotuc.ring-web.routes` provides
  - `:reitit.routes/api`: a reified route (derived from `:reitit/routes`)"
  (:require
   [aero.core :as aero]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [integrant.repl.state :as state]
   [lotuc.ring-web.config :refer [deep-merge]]
   [lotuc.ring-web.handler]
   [lotuc.ring-web.routes]))

(set! *warn-on-reflection* true)

(defonce system (atom nil))

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error {:what :uncaught-exception
                 :exception ex
                 :where (str "Uncaught exception on" (.getName thread))}))))

(defn system-config
  "Assume there is a `:rign-web-common` key which contains all the
  default configuration.

  There is a default one shipped in with ring-web, include with:

  ```edn
    :ring-web-common #include \"ring-web-common.edn\"
  ```"
  [config-resource aero-config]
  (let [{:keys [ring-web-common] :as config} (aero/read-config config-resource aero-config)]
    (deep-merge ring-web-common (-> config (dissoc :ring-web-common)))))

(defn stop-app []
  (some-> (deref system) (ig/halt!))
  (shutdown-agents))

(defn start-app [system-file given-opts]
  (->> (system-config system-file given-opts)
       (ig/prep)
       (ig/init)
       (reset! system))
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable stop-app)))

(defn system-state []
  (or @system state/system))

(defn system-fixture
  [system-file & [given-opts]]
  (fn [f]
    (when (nil? (system-state))
      (start-app system-file (or given-opts {:opts {:profile :test}})))
    (f)
    (when (some? @system)
      (stop-app))))
