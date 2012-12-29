(ns dire.test.core-test
  (:require [midje.sweet :refer :all]
            [dire.core :refer :all]))

(defn divider [a b]
  (/ a b))

(defhandler divider
  java.lang.ArithmeticException
  (fn [e & args] :division-by-zero-handler))

(defhandler divider
  java.lang.NullPointerException
  (fn [e & args] :npe-handler))

(fact (supervise divider 10 2) => 5)
(fact (supervise divider 10 0) => :division-by-zero-handler)
(fact (supervise divider 10 nil) => :npe-handler)

(defn unhandled-divider [a b]
  (/ a b))

(fact (with-out-str (supervise unhandled-divider 5 0))
      => "Untrapped exception: #<ArithmeticException java.lang.ArithmeticException: Divide by zero>\n")