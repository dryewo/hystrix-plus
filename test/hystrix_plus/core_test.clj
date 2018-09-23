(ns hystrix-plus.core-test
  (:require [clojure.test :refer :all]
            [hystrix-plus.core :refer :all]
            [com.netflix.hystrix.core :refer [defcommand]])
  (:import (hystrix_plus StackTraceUtils)))


(enable-full-command-stack-traces!)


(defn inner3 [] (/ 1 0))
(defn inner2 [] (inner3))
(defn inner1 [] (inner2))

(defcommand my-command []
  (inner1))

(defn outer1 [] @(com.netflix.hystrix.core/queue #'my-command))
(defn outer2 [] (outer1))
(defn outer3 [] (outer2))


(deftest works
  (testing "`outer1` is present in the stack trace"
    (try
      (outer3)
      (catch Exception e
        (is (some #(= "hystrix_plus.core_test$outer1" (.getClassName %)) (.getStackTrace e)))))))


(deftest about-isSameThreadThrowable
  (try
    (/ 1 0)
    (catch Exception e
      (is (true? (StackTraceUtils/isSameThreadThrowable e frames-to-skip))))))


(alter-var-root #'io.aviso.exception/*default-frame-rules*
                (constantly
                  [[:package #"^rx.*" :hide]
                   [:package #"^com\.netflix\.hystrix.*" :omit]
                   [:package #"^java\.util.*" :omit]
                   [:package #"^java\.lang.*" :omit]
                   [:name #"^clojure\.core.*" :omit]
                   ;; Standard rules
                   [:package "clojure.lang" :omit]
                   [:package #"sun\.reflect.*" :hide]
                   [:package "java.lang.reflect" :omit]
                   [:name #"speclj\..*" :terminate]
                   [:name #"clojure\.main/repl/read-eval-print.*" :terminate]]))
