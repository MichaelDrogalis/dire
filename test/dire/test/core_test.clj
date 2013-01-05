(ns dire.test.core-test
  (:require [midje.sweet :refer :all]
            [dire.core :refer :all]))

(defn divider [a b]
  (/ a b))

(with-handler #'divider
  "Catches divide by 0 errors."
  java.lang.ArithmeticException
  (fn [e & args] :division-by-zero-handler))

(with-handler #'divider
  java.lang.NullPointerException
  (fn [e & args] :npe-handler))

(fact (supervise #'divider 10 2) => 5)
(fact (supervise #'divider 10 0) => :division-by-zero-handler)
(fact (supervise #'divider 10 nil) => :npe-handler)

(with-finally #'divider
  "Prints the errors before finishing."
  (fn [& args]
    (apply println args)))

(fact (with-out-str (supervise #'divider 10 nil)) => "10 nil\n")

(defn unhandled-divider [a b]
  (/ a b))

(fact (supervise #'unhandled-divider 5 0) => (throws java.lang.ArithmeticException))

(defn add-one [n]
  (inc n))

(with-precondition #'add-one
   :not-two
  (fn [n & args]
    (not= n 2)))

(with-handler #'add-one
  {:precondition :not-two}
  (fn [e & args] (:precondition e)))

(fact (supervise #'add-one 2) => :not-two)
(fact (supervise #'add-one 0) => 1)

(defn subtract-one [n]
  (dec n))

(with-postcondition #'subtract-one
  :not-two
  (fn [n & args]
    (not= n 2)))

(with-handler #'subtract-one
  {:postcondition :not-two}
  (fn [e result] (str "Failed for " result)))

(fact (supervise #'subtract-one 3) => "Failed for 2")
(fact (supervise #'subtract-one 4) => 3)

