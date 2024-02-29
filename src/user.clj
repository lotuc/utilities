(ns user
  (:require
   [clojure.pprint :as pprint]
   [clojure.tools.namespace.repl :as repl]
   [hyperfiddle.rcf :as rcf]
   [integrant.repl]
   [integrant.repl.state]
   [lambdaisland.classpath.watch-deps :as watch-deps]
   [malli.dev]))

(add-tap (bound-fn* pprint/pprint))

(rcf/enable!)

(future (watch-deps/start! {:aliases []}))

(repl/set-refresh-dirs "src" "ring-web/src")

(malli.dev/start!)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(do
  (def refresh repl/refresh)
  (def reset integrant.repl/reset)
  (def go integrant.repl/go)
  (def halt integrant.repl/halt)
  (def system integrant.repl.state/system))
