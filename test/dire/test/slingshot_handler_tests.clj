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
  (if (or (string? object)
          (coll? object)
          (nil? object))
    (= (count object) 3)))

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


;; Test case where predicate function throws its own exception
(defn test-function2 [a]
  (throw+ a))

(defn test-bad-selector-predicate [object]
  (= (+ object 2) 4))

(with-handler!
  #'test-function2
  test-bad-selector-predicate
  (fn [e & args] "NUMBER THROWN"))

;; ## Slingshot Facts

;; Test supported selector types
(fact :slingshot (test-function "DATA") => "DATA")
(fact :slingshot (test-function "MAP") => "MAP THROWN")
(fact :slingshot (test-function "EXCEPTION") => "EXCEPTION THROWN")

(fact :slingshot (test-function "PREDICATE") => "LENGTH THREE VECTOR THROWN")

(fact :slingshot (test-function "UNHANDLED EXCEPTION") =>
      (throws Exception "UNHANDLED EXCEPTION THROWN"))

;; Test case selector throws its own exception
(defn predicate-selector-exception? [e]
  (if (instance? clojure.lang.ExceptionInfo e)
    (let [object (:object (ex-data e))
          type   (:type object)]
      (= type :dire.core/predicate-selector-exception))))

(fact :slingshot (test-function2 2) => "NUMBER THROWN")

(fact :slingshot (test-function2 "BAD ARG") => (throws predicate-selector-exception?))
