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

(with-pre-hook! #'multiply
  (fn [a b]
    (println "Logging" a "and" b)))

(fact (with-out-str (multiply 5 3)) => "Logging 5 and 3\n")

(defn eager-fn [x]
  (println "Body"))

(with-precondition! #'eager-fn
  "Precondition of x not being 0."
  :x-not-0
  (fn [x] (not= x 0)))

(with-handler! #'eager-fn
  "Handle the error."
  {:precondition :x-not-0}
  (fn [e x] (println "Handler")))

(with-eager-pre-hook! #'eager-fn
  "Logs something unconditionally."
  (fn [x] (println "Eager prehook")))

(with-pre-hook! #'eager-fn
  "Logs only after preconditions succeed."
  (fn [x] (println "Normal prehook")))

(fact (with-out-str (eager-fn 0)) => "Eager prehook\nHandler\n")

(defn add-two [x]
  (+ x 2))

(with-post-hook! #'add-two
  (fn [result] (println "Result was" result)))

(fact (with-out-str (add-two 0)) => "Result was 2\n")

