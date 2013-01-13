(ns dire.core
  (:require [slingshot.slingshot :refer [try+ throw+]]
            [robert.hooke :refer [add-hook]]))

(defn with-handler
  "Binds handler-fn as the receiver of an exception of type
   exception-type when task-var is invoked via supervise."
  ([task-var docstring? exception-type handler-fn]
     (with-handler task-var exception-type handler-fn))
  ([task-var exception-type handler-fn]
     (alter-meta! task-var assoc :error-handlers
                  (merge (:error-handlers (meta task-var) {})
                         {exception-type handler-fn}))))

(defn with-finally
  "Binds finally-fn as the last piece of code to execute within
   task-var, as if task-var were wrapped in a try/finally. task-var
   must be invoked via supervise."
  ([task-var docstring? finally-fn]
     (with-finally task-var finally-fn))
  ([task-var finally-fn]
     (alter-meta! task-var assoc :finally finally-fn)))

(defn with-precondition
  "Before task-var is invoked, pred-fn is evaluated with the
   original bindings to task-var. If it returns false, description is thrown.
   task-var must be invoked via supervise."
  [task-var description pred-fn]
  (alter-meta! task-var assoc :preconditions
               (assoc (:preconditions (meta task-var) {}) description pred-fn)))

(defn with-postcondition
  "After task-var is invoked, pred-fn is evaluated with the return value
   of task-var. If it return false, description is thrown. task-var must be
   invoked via supervise."
  [task-var description pred-fn]
  (alter-meta! task-var assoc :postconditions
               (assoc (:postconditions (meta task-var) {}) description pred-fn)))

(defn with-pre-hook [task-var f]
  "After task-var is invoked, preconditions are evaluated. If all preconditions
   return true, f is invoked. You can register any number of pre-hooks. They are
   not guaranteed to run in any specific order. Pre-hooks are useful for logging."
  (alter-meta! task-var assoc :pre-hooks
               (conj (:pre-hooks (meta task-var) #{}) f)))

(defn- eval-preconditions
  [task-metadata & args]
  (doseq [[pre-name pre-fn] (:preconditions task-metadata)]
    (when-not (apply pre-fn args)
      (throw+ {:type ::precondition :precondition pre-name}))))

(defn- eval-postconditions [task-metadata result & args]
  (doseq [[post-name post-fn] (:postconditions task-metadata)]
    (when-not (post-fn result)
      (throw+ {:type ::postcondition :postcondition post-name :result result}))))

(defn- eval-pre-hooks [task-metadata & args]
  (doseq [f (:pre-hooks task-metadata)]
    (apply f args)))

(defn- eval-finally [task-metadata & args]
  (when-let [finally-fn (:finally task-metadata)]
    (apply finally-fn args)))

(defn- default-error-handler [exception & _]
  (throw exception))

(defn- supervised-meta [task-meta task-var & args]
  (try+
   (apply eval-preconditions task-meta args)
   (apply eval-pre-hooks task-meta args)
   (let [result (apply task-var args)]
     (apply eval-postconditions task-meta result args)
     result)
   (catch [:type :dire.core/precondition] {:as conditions}
     (if-let [pre-handler (get (:error-handlers task-meta) {:precondition (:precondition conditions)})]
       (apply pre-handler conditions args)
       (throw+ conditions)))
   (catch [:type :dire.core/postcondition] {:as conditions}
     (if-let [post-handler (get (:error-handlers task-meta) {:postcondition (:postcondition conditions)})]
       (post-handler conditions (:result conditions))
       (throw+ conditions)))
   (catch Exception e
     (let [handler (get (:error-handlers task-meta) (type e) default-error-handler)]
       (apply handler e args)))
   (finally
    (apply eval-finally task-meta args))))

(defn supervise
  "Invokes task-var with args as the parameters. If any exceptions are raised,
   they are dispatched to any predefined handlers."
  [task-var & args]
  (apply supervised-meta (meta task-var) task-var args))

(defn- hook-supervisor-to-fn [task-var]
  (def supervisor (partial supervised-meta (meta task-var)))
  (add-hook task-var #'supervisor))

(defn with-handler!
  "Same as with-handler, but task-var can be invoked without supervise. (e.g. (task-var args))"
  [task-var exception-type handler-fn]
  (with-handler task-var exception-type handler-fn)
  (hook-supervisor-to-fn task-var))

(defn with-finally!
  "Same as with-finally, but task-var can be invoked without supervise."
  [task-var finally-fn]
  (with-finally task-var finally-fn)
  (hook-supervisor-to-fn task-var))

(defn with-precondition!
  "Same as with-precondition, but task-var can be invoked without supervise."
  [task-var description pred-fn]
  (with-precondition task-var description pred-fn)
  (hook-supervisor-to-fn task-var))

(defn with-postcondition!
  "Same as with-postcondition, but task-var can be invoked without supervise."
  [task-var description pred-fn]
  (with-postcondition task-var description pred-fn)
  (hook-supervisor-to-fn task-var))

(defn with-pre-hook!
  "Same as with-pre-hook, but task-var can be invoked without supervise."
  [task-var f]
  (with-pre-hook task-var f)
  (hook-supervisor-to-fn task-var))

