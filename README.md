# Robert Bruce

Robert Bruce provides an easy way to execute a function and allow
failures to be automatically retried.  It's named after
[Robert the Bruce](http://en.wikipedia.org/wiki/Robert_the_Bruce),
whose determination was inspired by the sight of a spider trying (and
failing) many times to build a web.

Add this to your project.clj :dependencies list:

    [robert/bruce "0.7.1"]

## Usage

```clojure
(use '[robert.bruce :only [try-try-again]])

;; arguments are like trampoline, if you want the default options
(try-try-again some-fn)
(try-try-again some-fn arg1 arg2)
(try-try-again #(some-fn arg1 arg2))

;; but with the addition of a first options arg, if you don't
(try-try-again {:sleep 5000 :tries 100} #(some-fn arg1 arg2))
(try-try-again {:sleep nil :tries 100} some-fn arg1 arg2)
(try-try-again {:decay :exponential :tries 100} #(some-fn arg1 arg2))

;; by default, a failure is detected when the function throws an
;; exception. options can refine that criterion based on the type of
;; exception thrown or by classifying some return values as failures.

(try-try-again {;; all options are optional

                ;; :sleep is used to specify how long to sleep
                ;; between retries, it can be a number, or false
                ;; or nil if you don't want to sleep,
                ;; default is 10 seconds (that is, 10000)
                :sleep 100

                ;; :tries is used to specific how many tries
                ;; will be attempted, it can also be :unlimited
                ;; default is 5
                :tries 100

                ;; if you want your sleep amount to change over
                ;; time, you can provide a decay function:
                ;; a number - your sleep will be multiplied by it, or
                ;; a function - the previous sleep value will passed
                ;;              into it, and you should return a new
                ;;              sleep. (that is, we'll do the state
                ;;              tracking internally for you.), or
                ;; a keyword - for out of the box decay algorithms
                ;;             :exponential, :double, :golden-ratio
                ;; default is identity

                ;; note: all these options are just for updating the
                ;; internal 'how long should i sleep?' state, the
                ;; actual sleeping is done elsewhere. see
                ;; `update-sleep` versus `retry` for more.

                :decay :exponential

                ;; if the function signals failure by its return value
                ;; rather than (or in addition to) throwing
                ;; exceptions, you can provide a return? predicate to
                ;; detect failures. The predicate will be given a
                ;; candidate return value and should return truthy if
                ;; the value should be returned or falsey to request a
                ;; retry. The :return? option value should be either:
                ;; a function of one argument, or
                ;; a keyword - for out of the box predicates:
                ;;             :always, :truthy?, :falsey?
                ;; default is :always
                :return? :truthy?

                ;; if you want to only retry when particular
                ;; exceptions are thrown, you can add a :catch
                ;; clause.  it works with either a single type
                ;; or a collection.
                ;; default is Exception
                :catch [java.io.IOException java.sql.SQLException]

               #(some-fn arg1 arg2))

;; When retries are exhausted, try-try-again will either:
;; - throw the last exception caught if the last try generated an
;;   exception, or
;; - return the last return value that the :return? predicate
;;   classified as indicating failure.

;; In addition, four dynamic variables are bound in both the
;; passed in and error-hook functions:
;; *first-try* - true if this is the first try
;; *last-try* - true if this is the last try
;; *try* - the current try number (starting at 1)
;; *error* - the last error that occurred
```

## License

Copyright (C) 2011 Joe Gallo

Distributed under the Eclipse Public License, the same as Clojure.
