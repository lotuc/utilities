(ns lotuc.clj.ioc-macros-hook)

(defmacro gen-plan
  [binds id-expr]
  (let [binds (partition 2 binds)
        psym (gensym "plan_")
        forms (reduce
               (fn [acc [id expr]]
                 (concat acc `[[~id ~psym] (~expr ~psym)]))
               []
               binds)]
    `(fn [~psym]
       (let [~@forms]
         [~id-expr ~psym]))))
