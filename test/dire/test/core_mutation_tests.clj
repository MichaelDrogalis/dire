(ns dire.test.core-mutation-tests
  (:require [midje.sweet :refer :all]
            [dire.core :refer :all]))

(defn multiply [a b]
  (* a b ))

(with-handler! #'multiply
  java.lang.NullPointerException
  (fn [e a b]
    :npe))

(fact (multiply 2 3) => 6)
(fact (multiply 1 nil) => :npe)

(with-precondition!
  #'multiply
  :not-equal
  (fn [a b]
    (not= a b)))

(with-handler! #'multiply
  {:precondition :not-equal}
  (fn [e & args] "Failed"))

(fact (multiply 2 2) => "Failed")

(with-postcondition!
  #'multiply
  :not-even
  (fn [result]
    (not (even? result))))

(with-handler! #'multiply
  {:postcondition :not-even}
  (fn [e result] "Post failed"))

(fact (multiply 3 2) => "Post failed")

