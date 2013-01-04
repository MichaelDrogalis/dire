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

