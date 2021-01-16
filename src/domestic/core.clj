(ns domestic.core
  (:require [clojure.inspector :refer [atom?]]))

(defmacro defevent
  "Creates an event for a given dispatcher"
  [dispatcher event-name args & fdecl]
  ;; Ignore the first argument which is the event name
  (let [args# (into ['_] args)]
    (if (instance? clojure.lang.Symbol dispatcher)
      `(defmethod ~dispatcher ~event-name
         ~args#
         (let [state# (second ~args#)]
           (do ~@fdecl)
           ;; always return state object to better support testing
           @state#))
      (throw (Exception. "defevent requires a valid dispatcher.")))))
