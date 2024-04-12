(ns lotuc.ring-web.db
  (:require
   [integrant.core :as ig]
   [next.jdbc.connection :as jdbc.connection])
  (:import
   [com.zaxxer.hikari HikariDataSource]))

(defmethod ig/init-key :db.sql/hikari-ds [_ opts]
  (jdbc.connection/->pool HikariDataSource opts))

(defmethod ig/halt-key! :db.sql/hikari-ds [_ ds]
  (.close ds))
