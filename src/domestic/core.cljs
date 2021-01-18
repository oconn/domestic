(ns domestic.core
  (:require [domestic.logger :refer [log-error]])
  (:require-macros [domestic.core]))

(defn bind-dispatcher
  "Binds a domestic dispathcer to it's state."
  [dispatcher state & constants]
  (if (instance? clojure.core/MultiFn dispatcher)
    #(apply dispatcher (first %) state (into (vec constants) (rest %)))
    (log-error "bind-dispatcher requires a MultiFn")))
