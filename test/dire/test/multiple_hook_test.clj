(ns dire.test.multiple-hook-test
  (:require [midje.sweet :refer :all]
            [dire.core :refer :all]))

(defn task-a [x] true)
(defn task-b [x y] true)

(with-pre-hook! #'task-a
  (fn [_]))

(with-pre-hook! #'task-b
  (fn [_ _]))

(fact (task-a true) => true)
(fact (task-b true true) => true)

(with-precondition! #'task-a
  :not-2
  (fn [x] (not= x 2)))

(with-handler! #'task-a
  {:precondition :not-2}
  (fn [e x] "Was 2"))

(with-precondition! #'task-a
  :not-4
  (fn [x] (not= x 4)))

(with-handler! #'task-a
  {:precondition :not-4}
  (fn [e x] "Was 4"))

(fact (task-a 2) => "Was 2")
(fact (task-a 4) => "Was 4")
(fact (task-a 1) => true)

(with-postcondition! #'task-a
  :not-5
  (fn [x] (not= x 5)))

(fact (task-a 5) => true)

(with-postcondition! #'task-a
  :not-true
  (fn [x] (not= x true)))

(with-handler! #'task-a
  {:postcondition :not-true}
  (fn [e x] "Was true"))

(fact (task-a true) => "Was true")

