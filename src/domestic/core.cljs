(ns domestic.core
  (:require-macros [domestic.core]))

(defn bind-dispatcher
  "Binds a domestic dispathcer to it's state."
  [dispatcher state & constants]
  #(apply dispatcher (first %) state (into (vec constants) (rest %))))
