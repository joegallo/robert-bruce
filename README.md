# Robert Bruce

Robert Bruce provides an easy way to execute a function and allow
failures to be automatically retried.  It's named after
[Robert the Bruce](http://en.wikipedia.org/wiki/Robert_the_Bruce),
whose determination was inspired by the sight of a spider trying (and
failing) many times to build a web.

Add this to your project.clj :dependencies list:

    [robert/bruce "0.8.0"]

## Usage

```clojure
(use '[robert.bruce :only [try-try-again]])

;; arguments are like trampoline, if you want the default options
(try-try-again some-fn)
(try-try-again some-fn arg1 arg2)
(try-try-again #(some-fn arg1 arg2))

;; but with the addition of a first options arg, if you don't
;; and seriously, you almost certainly don't want the default options. :)
(try-try-again {:sleep 5000 :tries 100} #(some-fn arg1 arg2))
(try-try-again {:sleep nil :tries 100} some-fn arg1 arg2)
(try-try-again {:decay :exponential :tries 100} #(some-fn arg1 arg2))
```

## Options

All options are optional -- but some have defaults that were, perhaps,
unwise choices.

### Basic Options

`:sleep` is used to specify how long to sleep between retries (in
milliseconds). It's expected to be a number, but could be false or nil
if you don't want to sleep at all. The default is 10 seconds (that is,
10000 milliseconds), and you should almost certainly override it. A
future version of robert-bruce will likely change the default value to
0 milliseconds.

`:tries` is used to specify how many tries will be attempted. it can
also be `:unlimited`. the default is 5.

`:catch` is to limit the exceptions that will be caught and retried
on. it is expected to be a single exception type, or a collection of
multiple exception types. the default is `java.lang.Exception`.

### Advanced Options

#### Decay

If you want your sleep amount to change over time, you can provide a
`:decay` function. This can be expressed in a few ways:

* a number - your sleep will be multiplied by it, or
* a function - the previous sleep value will passed
             into it, and you should return a new
             sleep. (that is, we'll do the state
             tracking internally for you.), or
* a keyword - for out of the box decay algorithms
            `:exponential`, `:double`, `:golden-ratio`

The default is identity -- that is, don't do any kind of decay.

Note: all these options are just for updating the internal 'how long
should i sleep?' state, the actual sleeping is done elsewhere. see
`update-sleep` versus `retry` for more.

#### Non-error returns

By default, a failure is detected when the function throws an
exception. Some options can refine that criterion based on the type of
exception thrown or by classifying some return values as failures.

For example, if the function signals failure by its return value
rather than (or in addition to) throwing exceptions, you can provide a
`:return?` predicate to detect failures. The predicate will be given a
candidate return value and should return truthy if the value should be
returned or falsey to request a retry. The `:return?` option value
should be either:

* a function of one argument, or
* a keyword - for out of the box predicates
            `:always`, `:truthy?`, `:falsey?`

The default is `:always` -- that is, return values aren't considered,
only thrown exceptions are.

#### Deciding when to retry

While retrying automatically is often sufficient, `:error-hook` is
provided for the case where you want to decide when to retry based on
the return value, exception caught, or other external factors.

`:error-hook` takes the form of a function that accepts the caught
exception or return value (when using `:return?`) as its single
argument.  It is called after each failure and can force a retry by
returning `true` or can cancel retries by returning `false`.

Note: `true` or `false` are checked explicitly.  All other values will
result in retries continuing as normal.

### Bound variables

Four dynamic variables are bound inside the scope of the try-try-again
(and, by extension, inside the function you're asking it to execute).

* `*first-try*` - true if this is the first try
* `*last-try*` - true if this is the last try
* `*try*` - the current try number (starting at 1)
* `*error*` - the last error that occurred

### Return values

If things succeed, then the successful return value is returned. If
retries are exhausted, then try-try-again will either:

* throw the last exception caught if the last try generated an
  exception, or
* return the last return value that the :return? predicate classified
  as indicating failure.

## License

Copyright (C) 2015 Joe Gallo

Distributed under the Eclipse Public License, the same as Clojure.
