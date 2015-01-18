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
