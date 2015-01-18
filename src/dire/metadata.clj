(ns dire.metadata
  "Functions to allow Dire hooks and conditions to be specified in the
  metadata of functions, and applied or removed automatically as
  needed."
  (:require [dire.core :refer :all]))

;;; Implementation functions
(defn- apply-dire-wrap-hook!
  [fn-var hook-fn]
  (with-wrap-hook! fn-var
    (-> hook-fn resolve meta :doc)
    hook-fn))

(defn- apply-dire-eager-pre-hook!
  [fn-var hook-fn]
  (with-eager-pre-hook! fn-var
    (-> hook-fn resolve meta :doc)
    hook-fn))

(defn- apply-dire-pre!
  [fn-var possible-handlers pre-cond-fn]
  (let [pre-cond-name (-> pre-cond-fn resolve meta ::pre-name)
        correct-handler (first (filter #(= pre-cond-name (-> % resolve meta ::pre-name))
                                       possible-handlers))]
    (with-precondition! fn-var
      (-> pre-cond-fn resolve meta :doc)
      pre-cond-name
      pre-cond-fn)
    (with-handler! fn-var
      (-> correct-handler resolve meta :doc)
      {:precondition pre-cond-name}
      correct-handler)))

(defn- remove-dire-wrap-hook!
  [fn-var hook-fn]
  (remove-wrap-hook! fn-var hook-fn))

(defn- remove-dire-eager-pre-hook!
  [fn-var hook-fn]
  (remove-eager-pre-hook! fn-var hook-fn))

(defn- remove-dire-pre!
  [fn-var pre-cond-fn]
  (let [pre-cond-name (-> pre-cond-fn resolve meta ::pre-name)]
    (remove-precondition! fn-var pre-cond-name)
    (remove-handler! fn-var {:precondition pre-cond-name})))

;;; Public API
(defn apply-dire-meta!
  [fn]
  (let [fn-var (resolve fn)
        preconditions (-> fn-var meta ::preconditions)
        pre-handlers (-> fn-var meta ::handlers ::pre-handlers)
        eager-pre-hooks (-> fn-var meta ::eager-pre-hooks)
        wrap-hooks (-> fn-var meta ::wrap-hooks)]
    (doall (map #(apply-dire-pre! fn-var pre-handlers %) preconditions))
    (doall (map #(apply-dire-eager-pre-hook! fn-var %) eager-pre-hooks))
    (doall (map #(apply-dire-wrap-hook! fn-var %) wrap-hooks))))

(defn remove-dire!
  [fn]
  (let [fn-var (resolve fn)
        preconditions (-> fn-var resolve meta ::preconditions)
        pre-handlers (-> fn-var resolve meta ::handlers ::pre-handlers)
        eager-pre-hooks (-> fn-var resolve meta ::eager-pre-hooks)
        wrap-hooks (-> fn-var resolve meta ::wrap-hooks)]
    (doall (map #(remove-dire-pre! fn-var %) preconditions))
    (doall (map #(remove-dire-eager-pre-hook! fn-var %) eager-pre-hooks))
    (doall (map #(remove-dire-wrap-hook! fn-var %) wrap-hooks))))

;;; Testing code
(defn precondition {::pre-name :pre} [a b] (prn "precond:") false)
(defn pre-handler {::pre-name :pre} [_ a b] (prn "handler:"))
(defn wrap-hook [r [a b]] (prn "wrap:"))
(defn eager-pre-hook [a b] (prn "eager:"))

(defn ^{::preconditions '[precondition]
        ::handlers {::pre-handlers '[pre-handler]}
        ::eager-pre-hooks '[eager-pre-hook]
        ::wrap-hooks '[wrap-hook]}
  test-fn
  "Docstring"
  [a b]
  (/ a b))
