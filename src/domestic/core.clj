(ns domestic.core)

(defmacro defevent
  "Creates an event for a given dispatcher"
  [dispatcher event-name args & fdecl]
  (if (instance? clojure.lang.Symbol dispatcher)
    `(defmethod ~dispatcher ~event-name
       ~args
       (let [state# (get ~args 2)]
         (do ~@fdecl)
         ;; always return state object to better support testing
         @state#))
    (throw (Exception. "defevent requires a valid dispatcher."))))
