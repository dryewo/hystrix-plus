# hystrix-plus
[![Build Status](https://travis-ci.org/dryewo/hystrix-plus.svg?branch=master)](https://travis-ci.org/dryewo/hystrix-plus)
[![Clojars Project](https://img.shields.io/clojars/v/me.dryewo/hystrix-plus.svg)](https://clojars.org/me.dryewo/hystrix-plus)

A companion library for [hystrix-clj] to enhance `HystrixRuntimeException`'s stack traces.

```clj
[me.dryewo/hystrix-plus "0.0.0"]
```

Please refer to _CHANGELOG.md_ for latest changes.

## Usage

Read next the section for the rationale.

Just add this library and put this call somewhere next to `(defn -main)`:

```clj
(ns my.main.ns
  (:require [hystrix-plus.core :as hystrix-plus]))

(hystrix-plus/enable-full-command-stack-traces!)
```

Now, when any Hystrix command throws an exception, the full call stack will be visible in the printout:

```clj
=> (outer3)
                                                                              ...                   
                                                                    user/eval2569      REPL Input   
                                                                              ...                   
                                                  hystrix-plus.core-test/eval2573  core_test.clj: 24
                                                    hystrix-plus.core-test/outer3  core_test.clj: 20
                                                    hystrix-plus.core-test/outer2  core_test.clj: 19
                                                    hystrix-plus.core-test/outer1  core_test.clj: 18
                                                                              ...                   
                                       hystrix-plus.core-test/eval2542/my-command  core_test.clj: 15
                                                                              ...                   
hystrix-plus.core/enable-full-command-stack-traces!/execute-and-join-stack-traces       core.clj: 44
                                                                              ...                   
                                       hystrix-plus.core-test/eval2542/my-command  core_test.clj: 16
                                                    hystrix-plus.core-test/inner1  core_test.clj: 13
                                                    hystrix-plus.core-test/inner2  core_test.clj: 12
                                                    hystrix-plus.core-test/inner3  core_test.clj: 11
                                                                              ...                   
                        java.lang.ArithmeticException: Divide by zero
com.netflix.hystrix.exception.HystrixRuntimeException: hystrix-plus.core-test/my-command failed and no fallback available.
          failureType: #object[com.netflix.hystrix.exception.HystrixRuntimeException$FailureType 0x77688a30 "COMMAND_EXCEPTION"]
    implementingClass: com.netflix.hystrix.core.proxy$com.netflix.hystrix.HystrixCommand$ff19274a
              clojure.lang.Compiler$CompilerException: com.netflix.hystrix.exception.HystrixRuntimeException: hystrix-plus.core-test/my-command failed and no fallback available., compiling:(/projects/hystrix-plus/test/hystrix_plus/core_test.clj:24:3)
```

## Rationale

Please get familiar with [Hystrix] purpose and main concepts before reading this.

Imagine that you have the following code:

```clj
(defn inner3 [] (/ 1 0))       ; Throws an exception
(defn inner2 [] (inner3))
(defn inner1 [] (inner2))

(defcommand my-command []      ; Executes the body on a separate thread
  (inner1))

(defn outer1 [] (my-command))
(defn outer2 [] (outer1))
(defn outer3 [] (outer2))
```

And then you call `(outer3)`:

```
=> (outer3)
                                       ...                   
hystrix-plus.core-test/eval2542/my-command  core_test.clj: 16
             hystrix-plus.core-test/inner1  core_test.clj: 13
             hystrix-plus.core-test/inner2  core_test.clj: 12
             hystrix-plus.core-test/inner3  core_test.clj: 11
                                       ...                   
                        java.lang.ArithmeticException: Divide by zero
com.netflix.hystrix.exception.HystrixRuntimeException: hystrix-plus.core-test/my-command failed and no fallback available.
          failureType: #object[com.netflix.hystrix.exception.HystrixRuntimeException$FailureType 0x77688a30 "COMMAND_EXCEPTION"]
    implementingClass: com.netflix.hystrix.core.proxy$com.netflix.hystrix.HystrixCommand$ff19274a
              clojure.lang.Compiler$CompilerException: com.netflix.hystrix.exception.HystrixRuntimeException: hystrix-plus.core-test/my-command failed and no fallback available., compiling:(/projects/hystrix-plus/test/hystrix_plus/core_test.clj:24:3)
```

(This example assumes that you use [io.aviso/pretty] for pretty printed stack traces).

Notice that `outer1`, `outer2`, `outer3` are missing from the `ArithmeticException` stack trace,
because the exception was created on a separate thread, where Hystrix executed its commands.
They are also not present in the wrapping `HystrixRuntimeException`'s stack trace, because [AbstractCommand.execute()](https://github.com/Netflix/Hystrix/blob/7f5a0afc23aa5ff82320560a04d4c81a45efd67c/hystrix-core/src/main/java/com/netflix/hystrix/HystrixCommand.java#L342)
just rethrows whatever the command thread has thrown.

Read more about [exception wrapping].

In the end, if someone sees this stack trace in logs, it would be very hard to find that `my-command`
was called by `outer1`, which in turn was called by `outer2`, which in turn was called by `outer3`.

### Semi-workaround

If instead of directly executing the command you `queue` it (obtaining a `Future`) and then immediately dereference it,
the resulting exception will include `outer1`, `outer2` and `outer3` in the stack trace of one of the wrapping exception's stack trace.

```clj
(defn outer1 [] @(com.netflix.hystrix.core/queue #'my-command))
```

`outer1`, `outer2` and `outer3` are not visible in [io.aviso/pretty] printout because it does not show the stack trace of `ExecutionException`.

```
=> (outer3)
                                       ...                   
hystrix-plus.core-test/eval2739/my-command  core_test.clj: 16
             hystrix-plus.core-test/inner1  core_test.clj: 13
             hystrix-plus.core-test/inner2  core_test.clj: 12
             hystrix-plus.core-test/inner3  core_test.clj: 11
                                       ...                   
                        java.lang.ArithmeticException: Divide by zero
com.netflix.hystrix.exception.HystrixRuntimeException: hystrix-plus.core-test/my-command failed and no fallback available.
          failureType: #object[com.netflix.hystrix.exception.HystrixRuntimeException$FailureType 0x77688a30 "COMMAND_EXCEPTION"]
    implementingClass: com.netflix.hystrix.core.proxy$com.netflix.hystrix.HystrixCommand$ff19274a
              java.util.concurrent.ExecutionException: Observable onError
              clojure.lang.Compiler$CompilerException: java.util.concurrent.ExecutionException: Observable onError, compiling:(/projects/hystrix-plus/test/hystrix_plus/core_test.clj:24:3)
```

With standard exception printing you can see `outer1`, `outer2` and `outer3`:

```
java.util.concurrent.ExecutionException: Observable onError, compiling:(/projects/hystrix-plus/test/hystrix_plus/core_test.clj:24:3)
	...
Caused by: java.util.concurrent.ExecutionException: Observable onError
	...
	at hystrix_plus.core_test$outer1.invokeStatic(core_test.clj:18)
	at hystrix_plus.core_test$outer1.invoke(core_test.clj:18)
	at hystrix_plus.core_test$outer2.invokeStatic(core_test.clj:19)
	at hystrix_plus.core_test$outer2.invoke(core_test.clj:19)
	at hystrix_plus.core_test$outer3.invokeStatic(core_test.clj:20)
	at hystrix_plus.core_test$outer3.invoke(core_test.clj:20)
	at hystrix_plus.core_test$eval1751.invokeStatic(core_test.clj:24)
	at hystrix_plus.core_test$eval1751.invoke(core_test.clj:24)
	...
Caused by: com.netflix.hystrix.exception.HystrixRuntimeException: hystrix-plus.core-test/my-command failed and no fallback available.
	...
Caused by: java.lang.ArithmeticException: Divide by zero
	...
	at hystrix_plus.core_test$inner3.invokeStatic(core_test.clj:11)
	at hystrix_plus.core_test$inner3.invoke(core_test.clj:11)
	at hystrix_plus.core_test$inner2.invokeStatic(core_test.clj:12)
	at hystrix_plus.core_test$inner2.invoke(core_test.clj:12)
	at hystrix_plus.core_test$inner1.invokeStatic(core_test.clj:13)
	at hystrix_plus.core_test$inner1.invoke(core_test.clj:13)
	at hystrix_plus.core_test$eval1722$my_command__1723.invoke(core_test.clj:16)
	...
```

~75% of the output omitted. This is better than nothing, but still doesn't make it too easy to find how the command was called.

This workaround has drawbacks:

* Is not compatible with [io.aviso/pretty]
* Requires to use `com.netflix.hystrix.core/queue` explicitly instead of just calling commands as functions in order to get full stack trace information.

## Implementation details

This library achieves this by replacing implementation of `com.netflix.hystrix.core/execute` with a function that
stitches together stack traces from the command thread and the current thread:

```clj
(fn execute-and-join-stack-traces [definition & args]
  (try
    (.execute ^HystrixExecutable (apply com.netflix.hystrix.core/instantiate definition args))
    (catch Exception e
      (extend-cross-thread-stack-trace! e)
      (throw e))))
```

It updates the stack trace of the [innermost exception][exception wrapping], `java.lang.ArithmeticException` in the example above
(because `io.aviso/pretty` only prints the innermost exception's stack trace).

Please check out `hystrix-plus.core/enable-full-command-stack-traces!` function source code for more details.


## License

Copyright © 2018 Dmitrii Balakhonskii

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


[hystrix-clj]: https://github.com/Netflix/Hystrix/tree/master/hystrix-contrib/hystrix-clj
[Hystrix]: https://github.com/Netflix/Hystrix/wiki
[io.aviso/pretty]: https://github.com/AvisoNovate/pretty
[exception wrapping]: http://tutorials.jenkov.com/java-exception-handling/exception-wrapping.html
