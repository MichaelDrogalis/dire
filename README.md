# dire <a href="https://travis-ci.org/MichaelDrogalis/dire"><img src="https://api.travis-ci.org/MichaelDrogalis/dire.png" /></a>

Decomplect error logic. Erlang-style supervisor error handling for Clojure. Inspired by the work of [Joe Armstrong](http://www.erlang.org/download/armstrong_thesis_2003.pdf).

## Installation

Available on Clojars:

    [dire "0.1.4"]

## Usage

### Simple Example
```clojure
(ns mytask
  (:require [dire.core :refer [defhandler supervise]]))

;;; Define a task to run. It's just a function.
(defn divider [a b]
  (/ a b))

;;; For a task, specify an exception that can be raised and a function to deal with it.
(defhandler divider
  "An optional docstring."
  java.lang.ArithmeticException
  ;;; 'e' is the exception object, 'args' are the original arguments to the task.
  (fn [e & args] (println "Cannot divide by 0.")))

(defhandler divider
  java.lang.NullPointerException
  (fn [e & args] (println "Ah! A Null Pointer Exception! Do something here!")))

;;; Invoke with the task name and it's arguments.
(supervise divider 10 0)   ; => "Cannot divide by 0."
(supervise divider 10 nil) ; => "Ah! A Null Pointer Exception! Do something here!"
```

### Self-Correcting Error Handling
```clojure
(ns mytask
  (:require [dire.core :refer [defhandler supervise]]
            [fs.core :refer [touch]]))

(defn read-file [file-name]
  (slurp file-name))

(defhandler read-file
  java.io.FileNotFoundException
  (fn [exception file-name & _]
    (touch file-name)
    (supervise read-file file-name)))

(supervise read-file "my-file")
```

### Try/Catch/Finally Semantics
```clojure
(defn add-one [n]
  (inc n))

(defhandler add-one
  java.lang.NullPointerException
  (fn [e & args] (println "Catching the exception.")))

(deffinally add-one
  (fn [& args] (println "Executing a finally clause.")))

(with-out-str (supervise add-one nil)) ; => "Catching the exception.\nExecuting a finally clause.\n"
```

### Preconditions
```clojure
(defn add-one [n]
  (inc n))

(defassertion add-one
  (fn [n & args]
    (not= n 2)))

(defhandler add-one
  java.lang.IllegalArgumentException
  (fn [e & args] (apply str "Precondition failure for argument list: " (vector args))))

(supervise add-one 2) ; => "Precondition failure for argument list: (2)"
```

If an exception is raised that has no handler, it will be raised up the stack like normal.

## License

Copyright © 2012 Michael Drogalis

Distributed under the Eclipse Public License, the same as Clojure.
