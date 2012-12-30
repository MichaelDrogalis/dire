(ns dire.core)

(defmacro defhandler
  ([task-name docstring? exception-type handler-fn]
     `(defhandler ~task-name ~exception-type ~handler-fn))
  ([task-name exception-type handler-fn]
     `(let [task-var# ~(resolve task-name)]
        (alter-meta! task-var#
                     assoc :error-handlers (merge (:error-handlers (meta task-var#) {})
                                                  {~exception-type ~handler-fn})))))

(defn default-error-handler [exception & _]  
  (throw exception))

(defmacro supervise [task-name & args]
  `(let [task-name# ~task-name
         task-var# ~(resolve task-name)]
     (try
       (task-name# ~@args)
       (catch Exception e#
         (let [handler# (get (:error-handlers (meta task-var#)) (type e#) default-error-handler)]
           (handler# e# ~@args))))))

