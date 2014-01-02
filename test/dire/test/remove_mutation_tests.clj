(ns dire.test.remove-functions-tests
  (:require [midje.sweet :refer :all]
            [dire.core :refer :all]
            [slingshot.slingshot :refer :all]))

(defn test-fn [a b]
  (/ a b))

(defn finally-fn [a b] (prn "finally:"))

(defn pre-hook [a b] (prn "pre:"))

(defn post-hook [r] (prn "post:"))

(defn eager-pre-hook [a b] (prn "eager:"))

(defn precondition [a b] (prn "precond:") true)

(defn postcondition [r] (prn "postcond:") true)

(with-finally! #'test-fn finally-fn)

(with-pre-hook! #'test-fn pre-hook)

(with-post-hook! #'test-fn post-hook)

(with-eager-pre-hook! #'test-fn eager-pre-hook)

(with-precondition! #'test-fn :pre precondition)

(with-postcondition! #'test-fn :post postcondition)

(defn strip [x]
  (apply str (filter #(and (not= \newline %) (not= \" %)) x)))

(fact (strip (with-out-str (test-fn 10 2))) => "eager:precond:pre:postcond:post:finally:")

(remove-finally! #'test-fn)
(fact (strip (with-out-str (test-fn 10 2))) => "eager:precond:pre:postcond:post:")

(remove-post-hook! #'test-fn post-hook)
(fact (strip (with-out-str (test-fn 10 2))) => "eager:precond:pre:postcond:")

(remove-postcondition! #'test-fn :post)
(fact (strip (with-out-str (test-fn 10 2))) => "eager:precond:pre:")

(remove-pre-hook! #'test-fn pre-hook)
(fact (strip (with-out-str (test-fn 10 2))) => "eager:precond:")

(remove-precondition! #'test-fn :pre)
(fact (strip (with-out-str (test-fn 10 2))) => "eager:")

(remove-eager-pre-hook! #'test-fn eager-pre-hook)
(fact (strip (with-out-str (test-fn 10 2))) => "")

(fact (test-fn 10 2) => 5)

