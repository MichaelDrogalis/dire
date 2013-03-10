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

