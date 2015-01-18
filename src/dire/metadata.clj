(ns dire.metadata
  "Functions to allow Dire hooks and conditions to be specified in the
  metadata of functions, and applied or removed automatically as
  needed."
  (:require [dire.core :refer :all]))

;;; Implementation functions
(defn- apply-dire-wrap-hook
  [fn-var hook-fn]
  (dire/with-wrap-hook! fn-var
    (-> hook-fn resolve meta :doc)
    hook-fn))

(defn- apply-dire-eager-pre-hook
  [fn-var hook-fn]
  (dire/with-eager-pre-hook! fn-var
    (-> hook-fn resolve meta :doc)
    hook-fn))

(defn- apply-dire-pre
  [fn-var possible-handlers pre-cond-fn]
  (let [pre-cond-name (-> pre-cond-fn resolve meta ::pre-name)
        correct-handler (first (filter #(= pre-cond-name (-> % resolve meta ::pre-name))
                                       possible-handlers))]
    (dire/with-precondition! fn-var
      (-> pre-cond-fn resolve meta :doc)
      pre-cond-name
      pre-cond-fn)
    (dire/with-handler! fn-var
      (-> correct-handler resolve meta :doc)
      {:precondition pre-cond-name}
      correct-handler)))
