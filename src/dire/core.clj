(ns dire.core
  (:require [slingshot.slingshot :refer [try+ throw+]]))

(defmacro defhandler
  ([task-name docstring? exception-type handler-fn]
     `(defhandler ~task-name ~exception-type ~handler-fn))
  ([task-name exception-type handler-fn]
     `(let [task-var# ~(resolve task-name)]
        (alter-meta! task-var# assoc :error-handlers
                     (merge (:error-handlers (meta task-var#) {})
                            {~exception-type ~handler-fn})))))

(defmacro deffinally
  ([task-name docstring? finally-fn]
     `(deffinally ~task-name ~finally-fn))
  ([task-name finally-fn]
     `(let [task-var# ~(resolve task-name)]
        (alter-meta! task-var# assoc :finally ~finally-fn))))

(defmacro defprecondition [task-name description pred-fn]
  `(let [task-var# ~(resolve task-name)]
     (alter-meta! task-var# assoc :preconditions
                  (assoc (:preconditions (meta task-var#) {}) ~description ~pred-fn))))

(defmacro defpostcondition [task-name description pred-fn]
  `(let [task-var# ~(resolve task-name)]
     (alter-meta! task-var# assoc :postconditions
                  (assoc (:postconditions (meta task-var#) {}) ~description ~pred-fn))))

(defn eval-preconditions! [task-metadata & args]
  (doseq [[pre-name pre-fn] (:preconditions task-metadata)]
    (when-not (apply pre-fn args)
      (throw+ {:type ::precondition :precondition pre-name}))))

(defn default-error-handler [exception & _]
  (throw exception))

(defmacro supervise [task-name & args]
  `(let [task-name# ~task-name
         task-var# ~(resolve task-name)]
     (try+
      (eval-preconditions! (meta task-var#) ~@args)
      (let [result# (task-name# ~@args)]
        (doseq [[post-name# post-fn#] (:postconditions (meta task-var#))]
          (when-not (post-fn# result#)
            (throw+ {:type ::postcondition :postcondition post-name# :result result#})))
        result#)
      (catch [:type :dire.core/precondition] {:as conditions#}
        (if-let [pre-handler# (get (:error-handlers (meta task-var#)) {:precondition (:precondition conditions#)})]
          (pre-handler# conditions# ~@args)
          (throw+ conditions#)))
      (catch [:type :dire.core/postcondition] {:as conditions#}
        (if-let [post-handler# (get (:error-handlers (meta task-var#)) {:postcondition (:postcondition conditions#)})]
          (post-handler# conditions# (:result conditions#))
          (throw+ conditions#)))
      (catch Exception e#
        (let [handler# (get (:error-handlers (meta task-var#)) (type e#) default-error-handler)]
          (handler# e# ~@args)))
      (finally
       (when-let [finally-fn# (:finally (meta task-var#))]
         (finally-fn# ~@args))))))

