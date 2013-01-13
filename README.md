# dire <a href="https://travis-ci.org/MichaelDrogalis/dire"><img src="https://api.travis-ci.org/MichaelDrogalis/dire.png" /></a>

Decomplect error logic. Erlang-style supervisor error handling for Clojure. Inspired by the work of [Joe Armstrong](http://www.erlang.org/download/armstrong_thesis_2003.pdf).

## Installation

Available on Clojars:

    [dire "0.2.0"]

## API

Check out the Codox API docs [here](http://michaeldrogalis.github.com/dire/).

## Usage

### Simple Example
```clojure
(ns mytask
  (:require [dire.core :refer [with-handler supervise]]))

;;; Define a task to run. It's just a function.
(defn divider [a b]
  (/ a b))

;;; For a task, specify an exception that can be raised and a function to deal with it.
(with-handler #'divider
  "An optional docstring."
  java.lang.ArithmeticException
  ;;; 'e' is the exception object, 'args' are the original arguments to the task.
  (fn [e & args] (println "Cannot divide by 0.")))

(with-handler #'divider
  java.lang.NullPointerException
  (fn [e & args] (println "Ah! A Null Pointer Exception! Do something here!")))

;;; Invoke with the task name and it's arguments.
(supervise #'divider 10 0)   ; => "Cannot divide by 0."
(supervise #'divider 10 nil) ; => "Ah! A Null Pointer Exception! Do something here!"
```

### Self-Correcting Error Handling
```clojure
(ns mytask
  (:require [dire.core :refer [with-handler supervise]]
            [fs.core :refer [touch]]))

(defn read-file [file-name]
  (slurp file-name))

(with-handler #'read-file
  java.io.FileNotFoundException
  (fn [exception file-name & _]
    (touch file-name)
    (supervise #'read-file file-name)))

(supervise #'read-file "my-file")
```

### Try/Catch/Finally Semantics
```clojure
(defn add-one [n]
  (inc n))

(with-handler #'add-one
  java.lang.NullPointerException
  (fn [e & args] (println "Catching the exception.")))

(with-finally #'add-one
  (fn [& args] (println "Executing a finally clause.")))

(with-out-str (supervise #'add-one nil)) ; => "Catching the exception.\nExecuting a finally clause.\n"
```

### Preconditions
```clojure
(defn add-one [n]
  (inc n))

(with-precondition #'add-one
  ;;; Name of the precondition
  :not-two
  (fn [n & args]
    (not= n 2)))

(with-handler #'add-one
  ;;; Pair of exception-type (:precondition) to the actual precondition (:not-two)
  {:precondition :not-two}
  (fn [e & args] (apply str "Precondition failure for argument list: " (vector args))))

(supervise #'add-one 2) ; => "Precondition failure for argument list: (2)"
```

### Postconditions
```clojure
(defn add-one [n]
  (inc n))

(with-postcondition #'add-one
  :not-two
  (fn [n & args]
    (not= n 2)))

(with-handler #'add-one
  {:postcondition :not-two}
  (fn [e result] (str "Postcondition failed for result: " result)))

(supervise #'add-one 1) ; => "Postcondition failed for result: 2"
```

### Pre-hooks
```clojure
(defn times [a b]
  (* a b))

(with-pre-hook #'times
  (fn [a b] (println "Logging something interesting."))

(supervise #'times 1 2) ; => "Logging something interesting.", 2
```

- Multiple pre-hooks evaluate in *arbitrary* order.
- There's no `with-post-hook`. You have `with-finally` for that.

### Look Ma! No Supervisor!
```clojure
(defn multiply [a b]
  (* a b))

;;; Note the '!'
(with-handler! #'multiply
  java.lang.NullPointerException
  (fn [e a b]
    :npe))

;;; Note, no call to 'supervise'. Just use the function
(multiply 1 nil) ; => :npe
```

### Etc
- `with-finally`, `with-precondition`, `with-postcondition`, and `with-pre-hook` all have similar bang variants as above.
- If an exception is raised that has no handler, it will be raised up the stack like normal.

## License

Copyright © 2012 Michael Drogalis

Distributed under the Eclipse Public License, the same as Clojure.

