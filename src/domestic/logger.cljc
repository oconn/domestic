(ns domestic.logger)

(def log-error
  #?(:cljs js/console.error
     :clj #(binding [*out* *err*]
             (println %))))
