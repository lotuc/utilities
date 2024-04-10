(ns fiddle.missionary.sample00
  (:require
   [missionary.core :as m]))

;;; https://clojurians.slack.com/archives/CL85MBPEF/p1712724063426379

(defmacro drain [flow] `(m/? (m/reduce (constantly nil) ~flow)))

;;; https://github.com/leonoel/missionary/blob/master/doc/tutorials/rx_comparison.md#f-merge
(defn interval [ms v]
  (let [now-ms #(System/currentTimeMillis)
        ms-seq (next (iterate (partial + ms) (now-ms)))]
    (m/ap (m/? (m/sleep (- (m/?> (m/seed ms-seq)) (now-ms)) v)))))

(defn batch [ms >f]
  (let [>g (m/ap (m/amb (m/?> >f) ::end))
        >s (interval ms ::sep)]
    (->> (m/ap (m/?> (m/?> 2 (m/seed [>g >s]))))
         (m/eduction
          (fn [rf]
            (let [!acc (atom [])]
              (completing
               (fn [r i]
                 (case i
                   ::sep (rf r (first (reset-vals! !acc [])))
                   ::end (reduced (rf r (first (reset-vals! !acc []))))
                   (swap! !acc conj i))))))))))

(defn interval-range [ms n]
  (->> (m/zip (fn [x & _] x) (m/seed (range)) (interval ms :emit))
       (m/eduction (take n))))

(comment
  (drain (m/ap (println (m/?> (interval-range 100 5)))))
  (drain (m/ap (println (m/?> (batch 1000 (interval-range 199 19)))))))
