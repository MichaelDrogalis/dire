(ns dire.test.core-test
  (:require [midje.sweet :refer :all]
            [dire.core :refer [defhandler deffinally defprecondition supervise]]))

(defn divider [a b]
  (/ a b))

(defhandler divider
  "Catches divide by 0 errors."
  java.lang.ArithmeticException
  (fn [e & args] :division-by-zero-handler))

(defhandler divider
  java.lang.NullPointerException
  (fn [e & args] :npe-handler))

(fact (supervise divider 10 2) => 5)
(fact (supervise divider 10 0) => :division-by-zero-handler)
(fact (supervise divider 10 nil) => :npe-handler)

(deffinally divider
  "Prints the errors before finishing."
  (fn [& args]
    (apply println args)))

(fact (with-out-str (supervise divider 10 nil)) => "10 nil\n")

(defn unhandled-divider [a b]
  (/ a b))

(fact (supervise unhandled-divider 5 0) => (throws java.lang.ArithmeticException))

(defn add-one [n]
  (inc n))

(defprecondition add-one
  :not-two
  (fn [n & args]
    (not= n 2)))

(defhandler add-one
  {:precondition :not-two}
  (fn [e & args] (:precondition e)))

(fact (supervise add-one 2) => :not-two)
(fact (supervise add-one 0) => 1)

