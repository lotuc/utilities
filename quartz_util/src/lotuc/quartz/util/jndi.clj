(ns lotuc.quartz.util.jndi
  (:import
   [java.util Hashtable]
   [javax.naming InitialContext]
   [javax.naming.spi NamingManager]
   [lotuc.quartz.util.jndi SimpleNamingContextBuilder]))

(defonce !simple-naming-context-bindings (delay (Hashtable.)))
(declare activate-simple-naming-context)
(defonce !simple-naming-context-builder (delay (activate-simple-naming-context @!simple-naming-context-bindings)))
(defonce !ctx (atom nil))

(defn activate-simple-naming-context
  ([] @!simple-naming-context-builder)
  ([^Hashtable bindings]
   (assert (not (NamingManager/hasInitialContextFactoryBuilder))
           "Cannot activate SimpleNamingContextBuilder: there is already a JNDI provider registered")
   (doto (SimpleNamingContextBuilder. bindings)
     (NamingManager/setInitialContextFactoryBuilder))))

(defn lookup [n]
  (.lookup (swap! !ctx #(or % (InitialContext.))) n))

(defn bind [n o]
  (.bind (swap! !ctx #(or % (InitialContext.))) n o))

(defn unbind [n]
  (.unbind (swap! !ctx #(or % (InitialContext.))) n))

(comment
  (activate-simple-naming-context)
  (bind "java:comp/UserTransaction" :a-user-transaction)
  (unbind "java:comp/UserTransaction")
  (lookup "java:comp/UserTransaction"))
