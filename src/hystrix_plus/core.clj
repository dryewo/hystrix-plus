(ns hystrix-plus.core
  (:require [com.netflix.hystrix.core])
  (:import (hystrix_plus StackTraceUtils)
           (com.netflix.hystrix HystrixExecutable)))


(defn get-innermost-exception [e]
  (if-let [cause (.getCause e)]
    (recur cause)
    e))


;; To exclude this library's functions from consideration when comparing and joining stack traces
(def frames-to-skip 4)


(defn extend-stack-trace [there-stack-trace]
  (when-not (nil? there-stack-trace)
    (let [there-length     (alength there-stack-trace)
          here-stack-trace (.getStackTrace (Throwable.))
          here-length      (alength here-stack-trace)
          element-type     (-> here-stack-trace .getClass .getComponentType)
          res-length       (-> (+ there-length here-length)
                               (- frames-to-skip))
          res              (make-array element-type res-length)]
      (System/arraycopy there-stack-trace 0 res 0 there-length)
      (System/arraycopy here-stack-trace frames-to-skip res there-length (- here-length frames-to-skip))
      res)))


(defn extend-cross-thread-stack-trace! [e]
  (let [innermost-exception (get-innermost-exception e)]
    (when-not (StackTraceUtils/isSameThreadThrowable innermost-exception frames-to-skip)
      (.setStackTrace innermost-exception (extend-stack-trace (.getStackTrace innermost-exception))))))


(defn enable-full-command-stack-traces! []
  (alter-var-root #'com.netflix.hystrix.core/execute
                  (constantly (fn execute-and-join-stack-traces [definition & args]
                                (try
                                  (.execute ^HystrixExecutable (apply com.netflix.hystrix.core/instantiate definition args))
                                  (catch Exception e
                                    (extend-cross-thread-stack-trace! e)
                                    (throw e)))))))
