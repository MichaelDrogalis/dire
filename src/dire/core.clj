(ns dire.core
  (:require [slingshot.slingshot :refer [try+ throw+]]
            [robert.hooke :refer [add-hook]]))

(defn with-handler
  "Binds handler-fn as the receiver of an exception of type
   exception-type when task-var is invoked via supervise."
  ([task-var docstring? exception-type handler-fn]
     (with-handler task-var exception-type handler-fn))
  ([task-var exception-type handler-fn]
     (alter-meta! task-var assoc-in [:dire/error-handlers exception-type] handler-fn)))

(defn with-finally
  "Binds finally-fn as the last piece of code to execute within
   task-var, as if task-var were wrapped in a try/finally. task-var
   must be invoked via supervise."
  ([task-var docstring? finally-fn]
     (with-finally task-var finally-fn))
  ([task-var finally-fn]
     (alter-meta! task-var assoc :dire/finally finally-fn)))

(defn with-precondition
  "Before task-var is invoked, pred-fn is evaluated with the
   original bindings to task-var. If it returns false, description is thrown.
   task-var must be invoked via supervise."
  ([task-var docstring? description pred-fn]
     (with-precondition task-var description pred-fn))
  ([task-var description pred-fn]
     (alter-meta! task-var assoc-in [:dire/preconditions description] pred-fn)))

(defn with-postcondition
  "After task-var is invoked, pred-fn is evaluated with the return value
   of task-var. If it return false, description is thrown. task-var must be
   invoked via supervise."
  ([task-var docstring? description pred-fn]
     (with-postcondition task-var description pred-fn))
  ([task-var description pred-fn]
     (alter-meta! task-var assoc-in [:dire/postconditions description] pred-fn)))

(defn with-pre-hook
  "After task-var is invoked, eager prehooks and preconditions are evaluated.
   If all preconditions return true, f is invoked. You can register any number of
   pre-hooks. They are not guaranteed to run in any specific order. Pre-hooks are
   useful for logging."
  ([task-var docstring? f]
     (with-pre-hook task-var f))
  ([task-var f]
     (alter-meta! task-var update-in [:dire/pre-hooks] (fnil conj #{}) f)))

(defn with-eager-pre-hook
  "After task-var is invoked, eager prehooks evaluated before preconditions,
   followed by prehooks. You can register any number of eager prehooks.
   They are not guaranteed to run in any specific order."
  ([task-var docstring? f]
     (with-eager-pre-hook task-var f))
  ([task-var f]
     (alter-meta! task-var update-in [:dire/eager-pre-hooks] (fnil conj #{}) f)))

(defn with-post-hook
  "After task-var is invoked and postconditions run, and before finally
   is invoked, post-hooks will run. You can register any number of post-hooks,
   and they are not guaranteed to run in any specific order. Useful for logging
   the result of the return value of a function."
  ([task-var docstring? f]
     (with-post-hook task-var f))
  ([task-var f]
     (alter-meta! task-var update-in [:dire/post-hooks] (fnil conj #{}) f)))

(defn- eval-preconditions
  [task-metadata & args]
  (doseq [[pre-name pre-fn] (:dire/preconditions task-metadata)]
    (when-not (apply pre-fn args)
      (throw+ {:type ::precondition :precondition pre-name}))))

(defn- eval-postconditions [task-metadata result & args]
  (doseq [[post-name post-fn] (:dire/postconditions task-metadata)]
    (when-not (post-fn result)
      (throw+ {:type ::postcondition :postcondition post-name :result result}))))

(defn- eval-eager-pre-hooks [task-metadata & args]
  (doseq [f (:dire/eager-pre-hooks task-metadata)]
    (apply f args)))

(defn- eval-pre-hooks [task-metadata & args]
  (doseq [f (:dire/pre-hooks task-metadata)]
    (apply f args)))

(defn- eval-post-hooks [task-metadata result]
  (doseq [f (:dire/post-hooks task-metadata)]
    (f result)))

(defn- eval-finally [task-metadata & args]
  (when-let [finally-fn (:dire/finally task-metadata)]
    (apply finally-fn args)))

(defn- default-error-handler [exception & _]
  (throw exception))

(defmulti apply-handler (fn [type & _] type))

(defmethod apply-handler :precondition [type task-meta conditions args]
  (if-let [pre-handler (get (:dire/error-handlers task-meta)
                            {:precondition (:precondition conditions)})]
    (apply pre-handler conditions args)
    (throw+ conditions)))

(defmethod apply-handler :postcondition [type task-meta conditions args]
  (if-let [post-handler (get (:dire/error-handlers task-meta)
                             {:postcondition (:postcondition conditions)})]
    (post-handler conditions (:result conditions))
    (throw+ conditions)))

(defmethod apply-handler :default [task-meta e args]
  (let [handler (get (:dire/error-handlers task-meta) (type e) default-error-handler)]
    (apply handler e args)))

(defn- supervised-meta [task-meta task-var & args]
  (try+
   (apply eval-eager-pre-hooks task-meta args)
   (apply eval-preconditions task-meta args)
   (apply eval-pre-hooks task-meta args)
   (let [result (apply task-var args)]
     (apply eval-postconditions task-meta result args)
     (eval-post-hooks task-meta result)
     result)
   (catch [:type :dire.core/precondition] {:as conditions}
     (apply-handler :precondition task-meta conditions args))
   (catch [:type :dire.core/postcondition] {:as conditions}
     (apply-handler :postcondition task-meta conditions args))
   (catch Exception e
     (apply-handler task-meta e args))
   (finally
    (apply eval-finally task-meta args))))

(defn supervise
  "Invokes task-var with args as the parameters. If any exceptions are raised,
   they are dispatched to any predefined handlers."
  [task-var & args]
  (apply supervised-meta (meta task-var) task-var args))

(defn- hook-supervisor-to-fn [task-var]
  (def supervisor# (partial supervised-meta (meta task-var)))
  (add-hook task-var :key supervisor#))

(defn with-handler!
  "Same as with-handler, but task-var can be invoked without supervise. (e.g. (task-var args))"
  ([task-var docstring? exception-type handler-fn]
     (with-handler! task-var exception-type handler-fn))
  ([task-var exception-type handler-fn]
     (with-handler task-var exception-type handler-fn)
     (hook-supervisor-to-fn task-var)))

(defn with-finally!
  "Same as with-finally, but task-var can be invoked without supervise."
  ([task-var docstring? finally-fn]
     (with-finally! task-var finally-fn))
  ([task-var finally-fn]
     (with-finally task-var finally-fn)
     (hook-supervisor-to-fn task-var)))

(defn with-precondition!
  "Same as with-precondition, but task-var can be invoked without supervise."
  ([task-var docstring? description pred-fn]
     (with-precondition! task-var description pred-fn))
  ([task-var description pred-fn]
     (with-precondition task-var description pred-fn)
     (hook-supervisor-to-fn task-var)))

(defn with-postcondition!
  "Same as with-postcondition, but task-var can be invoked without supervise."
  ([task-var docstring? description pred-fn]
     (with-postcondition! task-var description pred-fn))
  ([task-var description pred-fn]
     (with-postcondition task-var description pred-fn)
     (hook-supervisor-to-fn task-var)))

(defn with-pre-hook!
  "Same as with-pre-hook, but task-var can be invoked without supervise."
  ([task-var docstring? f]
     (with-pre-hook! task-var f))
  ([task-var f]
     (with-pre-hook task-var f)
     (hook-supervisor-to-fn task-var)))

(defn with-eager-pre-hook!
  "Same as with-eager-pre-hook, but task-var can be invoked without supervise."
  ([task-var docstring? f]
     (with-eager-pre-hook! task-var f))
  ([task-var f]
     (with-eager-pre-hook task-var f)
     (hook-supervisor-to-fn task-var)))

(defn with-post-hook!
  "Same as with-post-hook, but task-var can be invoked without supervise."
  ([task-var docstring? f]
     (with-post-hook! task-var f))
  ([task-var f]
     (with-post-hook task-var f)
     (hook-supervisor-to-fn task-var)))

