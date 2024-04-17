(ns lotuc.jdbc-util.honeysql)

(defn- merge-kvs
  ([old-kvs new-kvs]
   (merge-kvs old-kvs new-kvs (fn [_k _old new] new)))
  ([old-kvs new-kvs merge-fn]
   (assert (even? (count old-kvs)) (str old-kvs))
   (assert (even? (count new-kvs)) (str new-kvs))

   (let [kvs (partition 2 new-kvs)
         k->v (into {} (map vec kvs))
         [old-kvs' overritten-ks]
         (reduce (fn [[r replaced] [k v]]
                   (if-some [v' (k->v k)]
                     [(conj r k (merge-fn k v v')) (conj replaced k)]
                     [(conj r k v) replaced]))
                 [[] #{}]
                 (partition 2 old-kvs))]
     (reduce (fn [r [k v]] (if (overritten-ks k) r (conj r k v))) old-kvs' kvs))))

(defn join-by [q & args]
  (assert (even? (count args)) (str args))
  (let [!merged (atom #{})
        join-by (->> (fn [k old new]
                       (swap! !merged conj k)
                       (merge-kvs old new (fn [_ _ new] new)))
                     (merge-kvs (:join-by q) args))
        merged @!merged
        join-by (reduce
                 (fn [join-by [k v]]
                   (if (merged k)
                     join-by
                     (conj join-by k v)))
                 join-by
                 (partition 2 args))]
    (assoc q :join-by join-by)))

(defn join [q & args]
  (update-in q [:join] merge-kvs args))

(defn left-join [q & args]
  (update-in q [:left-join] merge-kvs args))

(defn right-join [q & args]
  (update-in q [:right-join] merge-kvs args))

(defn inner-join [q & args]
  (update-in q [:inner-join] merge-kvs args))

(defn outer-join [q & args]
  (update-in q [:outer-join] merge-kvs args))

(defn full-join [q & args]
  (update-in q [:full-join] merge-kvs args))

(defn cross-join [q & args]
  (update-in q [:cross-join] merge-kvs args))

(comment
  ;; deduplicate
  (merge-kvs
   [:a :condition1 :b :condition2]
   [:a :condition3 :c :condition4])

  (require '[honey.sql.helpers :as h])

  (-> {}
      (h/join-by :left [:a :condition1]
                 :right [:b :condition])
      (join-by :left [:b :condition2]))

  (-> {}
      (left-join :a :condition1 :b :condition2)
      (left-join :a :condition3 :c :condition4)))
