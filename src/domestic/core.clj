(ns domestic.core)

(defmacro defdispatcher
  "Creates a dispatcher"
  [dispatcher-name {:keys [_spec]}]
  `(do
     (defmulti ~dispatcher-name identity)
     (defmethod ~dispatcher-name :default [event-name#]
       (domestic.logger/log-error (str "Event '" event-name#
                                       "' is not registered on dispatcher '"
                                       '~dispatcher-name "'")))))

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
           (if (satisfies? cljs.core.IDeref state#)
             @state#
             state#)))
      (throw (Exception. "defevent requires a valid dispatcher.")))))
