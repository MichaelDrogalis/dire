(ns dire.test.dispatch-test
  (:require [midje.sweet :refer :all]
            [slingshot.slingshot :refer [throw+]]
            [dire.core :refer :all]))

(defn f [x] (throw+ {:type ::bad-param :x x}))

(with-dispatcher #'f
  (fn [e] (:type e)))

(with-handler #'f
  :dire.test.dispatch-test/bad-param
  (fn [e x] 42))

(fact (supervise #'f 0) => 42)

(defn g [x] (f x))

(with-dispatcher! #'g
  (fn [e] (:type e)))

(with-handler! #'g
  :dire.test.dispatch-test/bad-param
  (fn [e x] 64))

(fact (g 0) => 64)

