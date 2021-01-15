(ns domestic.core
  (:require-macros [domestic.core]))

(defn bind-dispatcher
  "Binds a domestic dispathcer to it's state."
  ([dispatcher state] (bind-dispatcher dispatcher state {}))
  ([dispatcher state context]
   #(apply dispatcher (first %) context state (rest %))))
