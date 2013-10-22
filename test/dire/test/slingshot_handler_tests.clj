(ns dire.test.slingshot-handler-tests
  (:require [midje.sweet :refer :all]
            [dire.core :refer :all]
            [slingshot.slingshot :refer :all]))

(defn test-function [arg]
  (cond
   (= arg "MAP") (throw+ {:type :custom-exception :message "MAP THROWN"})
   (= arg "EXCEPTION") (throw+ (java.lang.IllegalArgumentException. "EXCEPTION THROWN"))
   (= arg "PREDICATE") (throw+ [1 2 3])
   (= arg "UNHANDLED EXCEPTION") (throw+ (Exception. "UNHANDLED EXCEPTION THROWN"))
   :else arg))

(defn test-selector-predicate [object]
  (= (count object) 3))

;; Using slingshot selectors within with-handler
;; NOTE - Support for full selector forms is TBD  -- DP, 20-OCT-2013

;; Test slingshot match against key-values vector
(with-handler!
  #'test-function
  [:type :custom-exception]
  (fn [e & args] (:message e)))

;; Test slingshot match against class name
(with-handler!
  #'test-function
  java.lang.IllegalArgumentException
  (fn [e & args] (.getMessage e)))

;; Test slingshot match against predicate function
(with-handler!
  #'test-function
  test-selector-predicate
  (fn [e & args] "LENGTH THREE VECTOR THROWN"))


;; ## Slingshot Facts

(fact :slingshot (test-function "DATA") => "DATA")
(fact :slingshot (test-function "MAP") => "MAP THROWN")
(fact :slingshot (test-function "EXCEPTION") => "EXCEPTION THROWN")
(fact :slingshot (test-function "PREDICATE") => "LENGTH THREE VECTOR THROWN")

(fact :slingshot (test-function "UNHANDLED EXCEPTION") =>
      (throws Exception "UNHANDLED EXCEPTION THROWN"))
