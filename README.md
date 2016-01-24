# dire <a href="https://travis-ci.org/MichaelDrogalis/dire"><img src="https://api.travis-ci.org/MichaelDrogalis/dire.png" /></a>

Decomplect error logic. Error handling, pre/post conditions and general hooks for Clojure functions.

Ships with two flavors:

1. The drop-in style, using functions ending in '!'
2. Erlang-style inspired by the work of [Joe Armstrong](http://www.erlang.org/download/armstrong_thesis_2003.pdf) using a supervisor

## Installation

Available on Clojars:

    [dire "0.5.4"]

## API

Check out the Codox API docs [here](http://michaeldrogalis.github.com/dire/).

## Relevant Blog Posts
- [try/catch complects: We can do so much better](http://michaeldrogalis.tumblr.com/post/40181639419/try-catch-complects-we-can-do-so-much-better)
- [Beautiful Separation of Concerns](http://michaeldrogalis.tumblr.com/post/46560874730/beautiful-separation-of-concerns)

## Evaluation Order
1. Eager Pre-hooks
2. Preconditions
3. Pre-hooks
4. The target function
5. Exception handlers
6. Postconditions
7. Post-hooks
8. Finally clause

## Usage: Drop-in Flavor

### Simple Example
```clojure
(ns mytask
  (:require [dire.core :refer [with-handler!]]))

;;; Define a task to run. It's just a function.
(defn divider [a b]
  (/ a b))

;;; For a task, specify an exception that can be raised and a function to deal with it.
(with-handler! #'divider
  "Here's an optional docstring about the handler."
  java.lang.ArithmeticException
  ;;; 'e' is the exception object, 'args' are the original arguments to the task.
  (fn [e & args] (println "Cannot divide by 0.")))

(divider 10 0) ; => "Cannot divide by 0."
```

#### Multiple Exception Classes

Sometimes it is desirable to check for any one of a number of exceptions:

```clojure
(with-handler! #'divider
  "Here's an optional docstring about the handler."
  [java.lang.ArithmeticException,
   java.lang.NullPointerException]
  ;;; 'e' is the exception object, 'args' are the original arguments to the task.
  (fn [e & args] (println "Cannot divide by 0 or operate on nil values.")))

(divider 10 nil) ; => "Cannot divide by 0 or operate on nil values."
(divider 10 0)   ; => "Cannot divide by 0 or operate on nil values."
```

### Try/Catch/Finally Semantics
```clojure
(ns mytask
  (:require [dire.core :refer [with-handler! with-finally!]]))

;;; Define a task to run. It's just a function.
(defn divider [a b]
  (/ a b))

(with-handler! #'divider
  java.lang.ArithmeticException
  (fn [e & args] (println "Catching the exception.")))

(with-finally! #'divider
  "An optional docstring about the finally function."
  (fn [& args] (println "Executing a finally clause.")))

(divider 10 0) ; => "Catching the exception.\nExecuting a finally clause.\n"
```

### Slingshot Integration

#### Map Dispatch

```clojure
(ns my.ns
  (:require [dire.core :refer :all]
            [slingshot.slingshot :refer [throw+]]))

(defn f []
  (throw+ {:type :db-disconnection :id 42}))

(with-handler! #'f
  [:type :db-disconnection]
  (fn [e & args] "Safe and sound"))

(f) ;; => "Safe and sound"
```

#### Predicate Dispatch

```clojure
(defn f []
  (throw+ 42))

(with-handler! #'f
  even?
  (fn [e & args] "Caught it"))

(f) ;; => "Caught it"
```

### Preconditions
```clojure
(ns mytask
  (:require [dire.core :refer [with-precondition! with-handler!]]))

(defn add-one [n]
  (inc n))

(with-precondition! #'add-one
  "An optional docstring."
  ;;; Name of the precondition
  :not-two
  (fn [n & args]
    (not= n 2)))

(with-handler! #'add-one
  {:precondition :not-two}
  (fn [e & args] (apply str "Precondition failure for argument list: " (vector args))))

(add-one 2) ; => "Precondition failure for argument list: (2)"
```

### Postconditions
```clojure
(ns mytask
  (:require [dire.core :refer [with-postcondition! with-handler!]]))

(defn add-one [n]
  (inc n))

(with-postcondition! #'add-one
  "An optional docstring."
  ;;; Name of the postcondition
  :not-two
  (fn [n & args]
    (not= n 2)))

(with-handler! #'add-one
  {:postcondition :not-two}
  (fn [e result] (str "Postcondition failed for result: " result)))

(add-one 1) ; => "Postcondition failed for result: (2)"
```

### Pre-hooks
```clojure
(ns mydire.prehook
  (:require [dire.core :refer [with-pre-hook!]]))

(defn times [a b]
  (* a b))

(with-pre-hook! #'times
  "An optional docstring."
  (fn [a b] (println "Logging something interesting.")))

(times 21 2) ; => "Logging something interesting."
```

### Eager Pre-hooks
```clojure
(ns mydire.prehook
  (:require [dire.core :refer [with-eager-pre-hook!]]))

(defn times [a b]
  (* a b))

(with-eager-pre-hook! #'times
  "An optional docstring."
  (fn [a b] (println "Logging something before preconditions are evaluated.")))

(times 21 2) ; => "Logging something before preconditions are evaluated."
```

### Post-hooks
```clojure
(ns mydire.posthook
  (:require [dire.core :refer [with-post-hook!]]))

(defn times [a b]
  (* a b))

(with-post-hook! #'times
  "An optional docstring."
  (fn [result] (println "Result was" result)))

(times 21 2) ; => "Result was 42"
```

### Wrap-hooks
```clojure
(defn fake-db-query
  "A fake database query that sometimes fails"
  [query]
  (rand-nth [nil "valid-data"]))

(with-wrap-hook! #'fake-db-query
  "An optional docstring."
  (fn [result [query]]
    (if (not-empty result)
      result
      (println "Got the result" result "for input" query))))

(fake-db-query "select * from data") ; => "valid-data"
(fake-db-query "select * from data")
; => Got the result nil for input select * from data
; => nil
```

## Usage: Erlang Style with supervise

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

### Eager Pre-hooks
```clojure
(defn times [a b]
  (* a b))

(with-eager-pre-hook #'times
  "An optional docstring."
  (fn [a b] (println "Logging something before preconditions are evaluated.")))

(supervise #'times 21 2) ; => "Logging something before preconditions are evaluated."
```

### Post-hooks
```clojure
(defn times [a b]
  (* a b))

(with-post-hook #'times
  "An optional docstring."
  (fn [result] (println "Result was" result)))

(supervise #'times 21 2) ; => "Result was 42"
```

### Wrap-hooks
```clojure
(defn fake-db-query
  "A fake database query that sometimes fails"
  [query]
  (rand-nth [nil "valid-data"]))

(defn check-result
  [result [query]]
  (if (not-empty result)
    result
    (println "Got the result" result "for input" query)))

(supervise #'fake-db-query "select * from data") ; => "valid-data"
(supervise #'fake-db-query "select * from data")
; => Got the result nil for input select * from data
; => nil
```

## Etc
- If an exception is raised that has no handler, it will be raised up the stack like normal.
- Multiple pre-hooks evaluate in *arbitrary* order.

# Contributors
- [Stefan Edlich](https://github.com/edlich)
- [Jonathan Boston](https://github.com/bostonou)
- [Kasim Tuman](https://github.com/oneness)
- [Dylan Paris](https://github.com/dparis)
- [John Wregglesworth](https://github.com/johnworth)
- [Stephon Striplin](https://github.com/arsenerei)

## License

Copyright © 2012 Michael Drogalis

Distributed under the Eclipse Public License, the same as Clojure.

