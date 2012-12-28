# dire

Erlang-style supervisor error handling for Clojure 

## Installation

Available on Clojars:

    [dire "0.1.0-SNAPSHOT"]

## Usage

```clojure
(ns mytask
  (:require [dire.core :refer [deftask defhandler supervise]]))

(deftask divider [a b]
  (/ a b))

(defhandler divider
  java.lang.ArithmeticException
  (partial println "Cannot divide by 0."))

(defhandler divider
  java.lang.NullPointerException
  (partial println "Ah! A Null Pointer Exception! Do something here!"))

(supervise divider 10 0)```

## License

Copyright © 2012 Michael Drogalis

Distributed under the Eclipse Public License, the same as Clojure.
