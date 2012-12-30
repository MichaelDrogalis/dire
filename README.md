# dire

Decomplect error logic. Erlang-style supervisor error handling for Clojure. Inspired by the work of [Joe Armstrong](http://www.erlang.org/download/armstrong_thesis_2003.pdf).

## Installation

Available on Clojars:

    [dire "0.1.2-SNAPSHOT"]

## Usage

```clojure
(ns mytask
  (:require [dire.core :refer [defhandler supervise]]))

;;; Define a task to run. It's just a function.
(defn divider [a b]
  (/ a b))

;;; For a task, specify an exception that can be raised and a function to deal with it.
(defhandler divider
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

Self-correcting error handling with ease:
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

If an exception is raised that has no handler, it will be printed to `*out*`.

## License

Copyright © 2012 Michael Drogalis

Distributed under the Eclipse Public License, the same as Clojure.
