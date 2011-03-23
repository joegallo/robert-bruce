# Robert Bruce

Robert Bruce provides an easy way to execute a function and allow 
failures to be automatically retried.  It's named after 
[Robert the Bruce](http://en.wikipedia.org/wiki/Robert_the_Bruce),
whose determination was inspired by the sight a spider trying (and
failing) many times to build a web.

Add this to your project.clj :dependencies list:

    [robert/bruce "0.6.0"]

## Usage

    (use '[robert.bruce :only [try-try-again]])

    ;; arguments are like trampoline, if you want the default options
    (try-try-again some-fn)
    (try-try-again some-fn arg1 arg2)
    (try-try-again #(some-fn arg1 arg2))

    ;; but with the addition of a first options arg, if you don't
    (try-try-again {:sleep 5000 :tries 100} #(some-fn arg1 arg2))
    (try-try-again {:sleep nil :tries 100} #(some-fn arg1 arg2))
    (try-try-again {:decay :exponential :tries 100} #(some-fn arg1 arg2))

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
                    ;; a number - your sleep will be multiplied by it
                    ;; a function - your sleep will passed into it
                    ;; a keyword - for out of the box decay algorithms
                    ;;             :exponential, :double, :golden-ratio
                    ;; default is identity
                    :decay :exponential

                    ;; if you want to only retry when particular
                    ;; exceptions are thrown, you can add a :catch
                    ;; clause.  it works with either a single type
                    ;; or a collection.  
                    ;; default is Exception
                    :catch [java.io.IOException java.sql.SQLException]}
                   #(some-fn arg1 arg2))

    ;; you can also use metadata on the function itself
    (try-try-again ^{:decay :exponential :tries 100} #(some-fn arg1 arg2))

## License

Copyright (C) 2011 Joe Gallo

Distributed under the Eclipse Public License, the same as Clojure.
