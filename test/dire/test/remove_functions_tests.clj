(ns dire.test.remove-functions-tests
  (:require [midje.sweet :refer :all]
            [dire.core :refer :all]
            [slingshot.slingshot :refer :all]))

(defn test-fn [])

(def initial-test-fn-meta (meta (var test-fn)))

(defn apply-remove-handler! []
  (with-handler #'test-fn [:key :val] (fn []))
  (remove-handler #'test-fn [:key :val])
  (meta #'test-fn))

(defn apply-remove-finally! []
  (with-finally #'test-fn (fn []))
  (remove-finally #'test-fn)
  (meta #'test-fn))

(defn apply-remove-precondition! []
  (with-precondition #'test-fn :precond (fn []))
  (remove-precondition #'test-fn :precond)
  (meta #'test-fn))

(defn apply-remove-postcondition! []
  (with-postcondition #'test-fn :postcond (fn []))
  (remove-postcondition #'test-fn :postcond)
  (meta #'test-fn))

(defn apply-remove-pre-hook! []
  (let [hook (fn [])]
    (with-pre-hook #'test-fn hook)
    (remove-pre-hook #'test-fn hook)
    (meta #'test-fn)))

(defn apply-remove-eager-pre-hook! []
  (let [hook (fn [])]
    (with-eager-pre-hook #'test-fn hook)
    (remove-eager-pre-hook #'test-fn hook)
    (meta #'test-fn)))

(defn apply-remove-post-hook! []
  (let [hook (fn [])]
    (with-post-hook #'test-fn hook)
    (remove-post-hook #'test-fn hook)
    (meta #'test-fn)))


;; Grab the internal listing of hooks from the
;; robert.hooke library so that apply-remove-supervise
;; can be tested
(def robert-hooke-hooks #'robert.hooke/hooks)

(defn apply-remove-supervise! []
  (with-handler! #'test-fn [:key :val] (fn []))
  (remove-handler #'test-fn [:key :val])
  (remove-supervise #'test-fn)
  (robert-hooke-hooks #'test-fn))

;; Test remove-* functions

(fact :remove-fns (apply-remove-handler!) => initial-test-fn-meta)
(fact :remove-fns (apply-remove-finally!) => initial-test-fn-meta)
(fact :remove-fns (apply-remove-precondition!) => initial-test-fn-meta)
(fact :remove-fns (apply-remove-postcondition!) => initial-test-fn-meta)
(fact :remove-fns (apply-remove-pre-hook!) => initial-test-fn-meta)
(fact :remove-fns (apply-remove-eager-pre-hook!) => initial-test-fn-meta)
(fact :remove-fns (apply-remove-post-hook!) => initial-test-fn-meta)

(fact :remove-fns (apply-remove-supervise!) => nil)
