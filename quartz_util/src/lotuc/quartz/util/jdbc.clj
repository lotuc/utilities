(ns lotuc.quartz.util.jdbc
  (:import
   [java.sql Connection]
   [org.quartz.utils ConnectionProvider DBConnectionManager]))

(def ^:dynamic *connection* nil)

(defmacro override-delegate
  [type delegate & body]
  (let [d (gensym)
        overrides (group-by first body)
        methods (for [m (.getMethods (resolve type))
                      :let [f (-> (.getName m)
                                  symbol
                                  (with-meta {:tag (-> m .getReturnType .getName)}))]
                      :when (not (overrides f))
                      :let [args (for [t (.getParameterTypes m)]
                                   (with-meta (gensym) {:tag (.getName t)}))]]
                  (list f (vec (conj args 'this))
                        `(. ~d ~f ~@(map #(with-meta % nil) args))))]
    `(let [~d ~delegate]
       (reify ~type ~@body ~@methods))))

(defn override-connection-close [^Connection connection & [close commit]]
  #_{:clj-kondo/ignore [:unresolved-symbol]}
  (override-delegate
   Connection connection
   (close [this] (when close (close this)))
   (commit [this] (when commit (commit this)))))

(defn make-connection-provider [get-connection]
  (reify ConnectionProvider
    (getConnection [_]
      ;; notice that if *connection* is bound, it will be used, and we make sure
      ;; it cannot be commit/close by the Quartz (we're managing them ourselves)
      (or (some-> *connection* override-connection-close)
          (get-connection)))
    (shutdown [_])
    (initialize [_])))

(defn add-connection-provider [^String name get-connection]
  (doto (DBConnectionManager/getInstance)
    (.addConnectionProvider name (make-connection-provider get-connection))))
