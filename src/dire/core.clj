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

(defn default-error-handler [exception & _]
  (throw exception))

(defmacro supervise [task-name & args]
  `(let [task-name# ~task-name
         task-var# ~(resolve task-name)]
     (try+
       (doseq [[pre-name# pre-fn#] (:preconditions (meta task-var#))]
         (when-not (pre-fn# ~@args)
           (throw+ {:type ::precondition :precondition pre-name#})))
       (task-name# ~@args)
       (catch [:type :dire.core/precondition] {:as conditions#}
         ((get (:error-handlers (meta task-var#)) {:precondition (:precondition conditions#)}) conditions# ~@args))
       (catch Exception e#
         (let [handler# (get (:error-handlers (meta task-var#)) (type e#) default-error-handler)]
           (handler# e# ~@args)))
       (finally
        (when-let [finally-fn# (:finally (meta task-var#))]
          (finally-fn# ~@args))))))

(defn add-one [n]
  (inc n))

(defprecondition add-one
  :not-two
  (fn [n & args]
    (not= n 2)))

(defhandler add-one
  {:precondition :not-two}
  (fn [e & args] (apply str "Precondition failure for argument list: " (vector args))))

(supervise add-one 2)

