(ns lotuc.ring-web.quartz-tasks
  (:require
   [clojure.string :as string]
   [honey.sql :as sql]
   [honey.sql.helpers :as h]
   [integrant.core :as ig]
   [lotuc.quartz.util :as qu]
   [lotuc.ring-web.quartz-table-schemas :as quartz-table-schemas]
   [malli.core :as malli]
   [malli.transform :as mt]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [ring.util.http-response :as http-response])
  (:import
   [java.util Date]
   [org.quartz JobExecutionContext]))

(defn job-fn [^JobExecutionContext ctx data-map]
  (Thread/sleep 100)
  (println (with-out-str
             (print (str "[" (.. ctx (getJobDetail) (getKey)) "]"))
             (print "[stateful] Hello Quartz!" data-map (Date.))))
  {:result "some-result"
   ;; for stateful job, this will replace the Job's data map
   ;; if you want to modify the Job's data map with `ctx`, leave this out
   :data-map (update data-map "count" (fnil inc 0))})

(defmethod ig/init-key :quartz/scheduler [_ _]
  (doto (qu/make-scheduler {:threadPool.threadCount 4})
    (qu/schedule-job
     {:key ["lotuc" "job-42"] :stateful `job-fn}
     {:key ["lotuc" "trigger-42"]
      :type :simple :interval 10e3 :repeat 100})
    (.start)))

(defmethod ig/halt-key! :quartz/scheduler [_ s]
  (when s (.shutdown s)))

(derive :reitit.routes.api/quartz :reitit.routes.api/routes)

(defn- mysql-execute [q {:keys [db]}]
  (->> {:return-keys true :builder-fn rs/as-unqualified-kebab-maps}
       (jdbc/execute! db (sql/format q {:dialect :mysql :quoted false}))))

(defn decode-table-row [table-name row]
  (some-> (quartz-table-schemas/table-schemas table-name)
          (malli/decode row (mt/transformer
                             mt/strip-extra-keys-transformer
                             mt/string-transformer))
          ((fn [v] (into {} (filter (comp some? second) v))))))

(defn list-qrtz-table [ctx table-name]
  (let [n (-> (str "qrtz-" (name table-name))
              (string/upper-case)
              (string/replace #"-" "_")
              keyword)]
    (-> (h/select :*)
        (h/from n)
        (mysql-execute ctx)
        ((partial map (partial decode-table-row table-name))))))

(def list-qrtz-table-query
  [:map [:table-name (into [:enum] (keys quartz-table-schemas/table-schemas))]])

(def list-qrtz-table-response
  (into [:multi {:dispatch :table-name}]
        (map (fn [table-name]
               [table-name [:map
                            [:table-name [:enum table-name]]
                            [:status [:enum "ok"]]
                            [:data [:vector (quartz-table-schemas/table-schemas table-name)]]]])
             (keys quartz-table-schemas/table-schemas))))

(defn list-qrtz-table-handler [{:keys [ctx] :as req}]
  (let [n (get-in req [:parameters :query :table-name])
        r (list-qrtz-table ctx n)]
    (http-response/ok {:status "ok" :table-name n :data r})))

(defmethod ig/init-key :reitit.routes.api/quartz
  [_ {:keys [base-path] :as opts}]
  (let [ctx (select-keys opts [:db])
        with-ctx (fn [h] #(h (assoc % :ctx ctx)))]
    [(or base-path "")
     {:middleware [with-ctx]}
     ["/quartz/:table-name"
      {:get {:handler list-qrtz-table-handler
             :parameters {:query list-qrtz-table-query}
             :responses {200 {:body list-qrtz-table-response}}}}]]))
