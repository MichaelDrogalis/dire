(ns dire.core
  (:require [clojure.core.incubator :refer [dissoc-in]]
            [robert.hooke :refer [add-hook remove-hook]]
            [slingshot.slingshot :refer [try+ throw+]]))

(defn- disj-from-set-at-key
  "Calls disj on a set keyed by mk in m in order to remove key sk.
   Dissoc mk from m if set is empty after disj."
  [m mk sk]
  (let [s (disj (get m mk) sk)]
    (if (empty? s)
      (dissoc m mk)
      (assoc m mk s))))

(defn with-handler
  "Binds handler-fn as the receiver of an exception which matches
   exception-selector when task-var is invoked via supervise."
  ([task-var docstring? exception-selector handler-fn]
     (with-handler task-var exception-selector handler-fn))
  ([task-var exception-selector handler-fn]
     (alter-meta! task-var assoc-in [:dire/error-handlers exception-selector] handler-fn)))

(defn remove-handler
  "Removes from task-var any handler registered with exception-selector."
  [task-var exception-selector]
  (alter-meta! task-var dissoc-in [:dire/error-handlers exception-selector]))

(defn with-finally
  "Binds finally-fn as the last piece of code to execute within
   task-var, as if task-var were wrapped in a try/finally. task-var
   must be invoked via supervise."
  ([task-var docstring? finally-fn]
     (with-finally task-var finally-fn))
  ([task-var finally-fn]
     (alter-meta! task-var assoc :dire/finally finally-fn)))

(defn remove-finally
  "Removes from task-var any registered finally function."
  [task-var]
  (alter-meta! task-var dissoc :dire/finally))

(defn with-precondition
  "Before task-var is invoked, pred-fn is evaluated with the
   original bindings to task-var. If it returns false, description is thrown.
   task-var must be invoked via supervise."
  ([task-var docstring? description pred-fn]
     (with-precondition task-var description pred-fn))
  ([task-var description pred-fn]
     (alter-meta! task-var assoc-in [:dire/preconditions description] pred-fn)))

(defn remove-precondition
  "Removes from task-var any precondition matching description."
  [task-var description]
  (alter-meta! task-var dissoc-in [:dire/preconditions description]))

(defn with-postcondition
  "After task-var is invoked, pred-fn is evaluated with the return value
   of task-var. If it return false, description is thrown. task-var must be
   invoked via supervise."
  ([task-var docstring? description pred-fn]
     (with-postcondition task-var description pred-fn))
  ([task-var description pred-fn]
     (alter-meta! task-var assoc-in [:dire/postconditions description] pred-fn)))

(defn remove-postcondition
  "Removes from task-var any postcondition matching description."
  [task-var description]
  (alter-meta! task-var dissoc-in [:dire/postconditions description]))

(defn with-pre-hook
  "After task-var is invoked, eager prehooks and preconditions are evaluated.
   If all preconditions return true, f is invoked. You can register any number of
   pre-hooks. They are not guaranteed to run in any specific order. Pre-hooks are
   useful for logging."
  ([task-var docstring? f]
     (with-pre-hook task-var f))
  ([task-var f]
     (alter-meta! task-var update-in [:dire/pre-hooks] (fnil conj #{}) f)))

(defn remove-pre-hook
  "Removes any registered pre-hook function f from task-var."
  [task-var f]
  (alter-meta! task-var disj-from-set-at-key :dire/pre-hooks f))

(defn with-eager-pre-hook
  "After task-var is invoked, eager prehooks evaluated before preconditions,
   followed by prehooks. You can register any number of eager prehooks.
   They are not guaranteed to run in any specific order."
  ([task-var docstring? f]
     (with-eager-pre-hook task-var f))
  ([task-var f]
     (alter-meta! task-var update-in [:dire/eager-pre-hooks] (fnil conj #{}) f)))

(defn remove-eager-pre-hook
  "Removes any registered eager pre-hook function f from task-var."
  [task-var f]
  (alter-meta! task-var disj-from-set-at-key :dire/eager-pre-hooks f))

(defn with-post-hook
  "After task-var is invoked and postconditions run, and before finally
   is invoked, post-hooks will run. You can register any number of post-hooks,
   and they are not guaranteed to run in any specific order. Useful for logging
   the result of the return value of a function."
  ([task-var docstring? f]
     (with-post-hook task-var f))
  ([task-var f]
     (alter-meta! task-var update-in [:dire/post-hooks] (fnil conj #{}) f)))

(defn remove-post-hook
  "Removes any registered post-hook function f from task-var."
  [task-var f]
  (alter-meta! task-var disj-from-set-at-key :dire/post-hooks f))

(defn with-wrap-hook
  "After task-var is invoked and postconditions run, wrap-hooks will
   run before post-hooks. You can register any number of wrap-hooks,
   and they are not guaranteed to run in any specific order. Useful for
  logging the return value of a function alongside its arguments."
  ([task-var docstring? f]
     (with-wrap-hook task-var f))
  ([task-var f]
     (alter-meta! task-var update-in [:dire/wrap-hooks] (fnil conj #{}) f)))

(defn remove-wrap-hook
  "Removes any registered wrap-hook function f from task-var."
  [task-var f]
  (alter-meta! task-var disj-from-set-at-key :dire/wrap-hooks f))

(defn- eval-preconditions
  [task-metadata & args]
  (doseq [[pre-name pre-fn] (:dire/preconditions task-metadata)]
    (when-not (apply pre-fn args)
      (throw+ {:type ::precondition :precondition pre-name}))))

(defn- eval-postconditions [task-metadata result & args]
  (doseq [[post-name post-fn] (:dire/postconditions task-metadata)]
    (when-not (post-fn result)
      (throw+ {:type ::postcondition :postcondition post-name :result result}))))

(defn- eval-wrap-hooks
  [task-metadata result & args]
  (doseq [f (:dire/wrap-hooks task-metadata)]
    (f result args)))

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

(defn- selector-type [selector]
  (cond
   (class? selector)
   :class-name

   (and (vector? selector)
        (even? (count selector)))
   :key-values

   (fn? selector)
   :predicate))

(defmulti selector-matches?
  (fn [selector _] (selector-type selector))
  :default :unknown-selector)

(defmethod selector-matches? :class-name [selector object]
  (instance? selector object))

(defmethod selector-matches? :key-values [selector object]
  (let [key-val-tests (for [[key val] (partition 2 selector)]
                        (= (get object key) val))]
    (every? true? key-val-tests)))

(defmethod selector-matches? :predicate [selector object]
  (try
    (selector object)
    (catch Throwable e
      (throw+ {:type ::predicate-selector-exception :predicate-arg object}))))

(defmethod selector-matches? :unknown-selector [selector object]
  false)

(defn- match-handler-selector [handlers object]
  (let [[[selector _]] (seq handlers)]
    (cond
     (empty? handlers) nil
     (selector-matches? selector object) selector
     :else (recur (rest handlers) object))))

(defn default-error-handler [exception & _]
  (throw+ exception))

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

(defmethod apply-handler :default [task-meta thrown-object args]
  (let [error-handlers (:dire/error-handlers task-meta)
        handler        (get error-handlers
                            (match-handler-selector error-handlers
                                                    thrown-object)
                            default-error-handler)]
    (apply handler thrown-object args)))

(defn- generate-catch-predicate
  "Creates a predicate closure around the task context which returns a boolean
   indicating whether the object tested matches any known error handler
   selectors."
  [task-meta]
  (let [handlers (:dire/error-handlers task-meta)]
    (fn [object]
      (boolean (match-handler-selector handlers object)))))

(defn- supervised-meta [task-meta task-var & args]
  (try+
   (apply eval-eager-pre-hooks task-meta args)
   (apply eval-preconditions task-meta args)
   (apply eval-pre-hooks task-meta args)
   (let [result (apply task-var args)]
     (apply eval-postconditions task-meta result args)
     (apply eval-wrap-hooks task-meta result args)
     (eval-post-hooks task-meta result)
     result)
   (catch [:type :dire.core/precondition] {:as conditions}
     (apply-handler :precondition task-meta conditions args))
   (catch [:type :dire.core/postcondition] {:as conditions}
     (apply-handler :postcondition task-meta conditions args))
   (catch (generate-catch-predicate task-meta) e
     (apply-handler task-meta e args))
   (finally
    (apply eval-finally task-meta args))))

(defn supervise
  "Invokes task-var with args as the parameters. If any exceptions are raised,
   they are dispatched to any predefined handlers."
  [task-var & args]
  (apply supervised-meta (meta task-var) task-var args))

(defn hook-supervisor-to-fn [task-var]
  (def supervisor# (partial supervised-meta (meta task-var)))
  (add-hook task-var ::supervisor-hook-key supervisor#))

(defn remove-supervise
  "Clears supervise hook from task-var."
  [task-var]
  (remove-hook task-var ::supervisor-hook-key))

(defn with-handler!
  "Same as with-handler, but task-var can be invoked without supervise. (e.g. (task-var args))"
  ([task-var docstring? exception-selector handler-fn]
     (with-handler! task-var exception-selector handler-fn))
  ([task-var exception-selector handler-fn]
     (with-handler task-var exception-selector handler-fn)
     (hook-supervisor-to-fn task-var)))

(defn remove-handler!
  "Removes from task-var any handler registered with exception-selector
   through with-handler!"
  [task-var exception-selector]
  (remove-handler task-var exception-selector)
  (remove-supervise task-var)
  (hook-supervisor-to-fn task-var))

(defn with-finally!
  "Same as with-finally, but task-var can be invoked without supervise."
  ([task-var docstring? finally-fn]
     (with-finally! task-var finally-fn))
  ([task-var finally-fn]
     (with-finally task-var finally-fn)
     (hook-supervisor-to-fn task-var)))

(defn remove-finally!
  "Removes from task-var a registered finally clause through with-finally!"
  [task-var]
  (remove-finally task-var)
  (remove-supervise task-var)
  (hook-supervisor-to-fn task-var))

(defn with-precondition!
  "Same as with-precondition, but task-var can be invoked without supervise."
  ([task-var docstring? description pred-fn]
     (with-precondition! task-var description pred-fn))
  ([task-var description pred-fn]
     (with-precondition task-var description pred-fn)
     (hook-supervisor-to-fn task-var)))

(defn remove-precondition!
  "Removes from task-var a precondition matching description registered
   through with-precondition!"
  [task-var description]
  (remove-precondition task-var description)
  (remove-supervise task-var)
  (hook-supervisor-to-fn task-var))

(defn with-postcondition!
  "Same as with-postcondition, but task-var can be invoked without supervise."
  ([task-var docstring? description pred-fn]
     (with-postcondition! task-var description pred-fn))
  ([task-var description pred-fn]
     (with-postcondition task-var description pred-fn)
     (hook-supervisor-to-fn task-var)))

(defn remove-postcondition!
  "Removes from task-var a postcondition matching description registered
   through with-postcondition!"
  [task-var description]
  (remove-postcondition task-var description)
  (remove-supervise task-var)
  (hook-supervisor-to-fn task-var))

(defn with-pre-hook!
  "Same as with-pre-hook, but task-var can be invoked without supervise."
  ([task-var docstring? f]
     (with-pre-hook! task-var f))
  ([task-var f]
     (with-pre-hook task-var f)
     (hook-supervisor-to-fn task-var)))

(defn remove-pre-hook!
  "Removes any pre-hook function f from task-var registered through
   with-pre-hook!"
  [task-var f]
  (remove-pre-hook task-var f)
  (remove-supervise task-var)
  (hook-supervisor-to-fn task-var))

(defn with-eager-pre-hook!
  "Same as with-eager-pre-hook, but task-var can be invoked without supervise."
  ([task-var docstring? f]
     (with-eager-pre-hook! task-var f))
  ([task-var f]
     (with-eager-pre-hook task-var f)
     (hook-supervisor-to-fn task-var)))

(defn remove-eager-pre-hook!
  "Removes any eager pre-hook function f from task-var registered through
   with-eager-pre-hook!"
  [task-var f]
  (remove-eager-pre-hook task-var f)
  (remove-supervise task-var)
  (hook-supervisor-to-fn task-var))

(defn with-post-hook!
  "Same as with-post-hook, but task-var can be invoked without supervise."
  ([task-var docstring? f]
     (with-post-hook! task-var f))
  ([task-var f]
     (with-post-hook task-var f)
     (hook-supervisor-to-fn task-var)))

(defn remove-post-hook!
  "Removes any post-hook function f from task-var registered through
   with-post-hook!"
  [task-var f]
  (remove-post-hook task-var f)
  (remove-supervise task-var)
  (hook-supervisor-to-fn task-var))

(defn with-wrap-hook!
  "Same as with-wrap-hook, but task-var can be invoked without supervise."
  ([task-var docstring? f] (with-wrap-hook! task-var f))
  ([task-var f]
     (with-wrap-hook task-var f)
     (hook-supervisor-to-fn task-var)))

(defn remove-wrap-hook!
  "Removes any wrap-hook function f from task-var registered through
   with-wrap-hook!"
  [task-var f]
  (remove-wrap-hook task-var f)
  (remove-supervise task-var)
  (hook-supervisor-to-fn task-var))

