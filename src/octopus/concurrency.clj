(ns octopus.concurrency
  (:require [clojure.core.async :as a]))


;; Concurrency


;; Let's define a function that launches n go blocks and runs a function f in each go block.
(defn launch-n-go-blocks [n f]
  (let [<c (a/chan)]
    ;; producer
    (dotimes [i n]
      (a/go (f) (a/>! <c i)))
    ;; consumer
    (loop [i 0]
      (when-not (= i n) (a/<!! <c) (recur (inc i))))))

;; Now run it for 8, with a simulated one second blocking function.
(time (launch-n-go-blocks 8 #(Thread/sleep 1000))) ;; 1005ms

;; Now run it for 9, with a simulated one second blocking function.
(time (launch-n-go-blocks 9 #(Thread/sleep 1000))) ;; 2004ms

;; Now run it for 16, with a simulated one second blocking function.
(time (launch-n-go-blocks 16 #(Thread/sleep 1000))) ;; 2010ms

;; Now run it for 17, with a simulated one second blocking function.
(time (launch-n-go-blocks 17 #(Thread/sleep 1000))) ;; 3008ms

;; clearly, there are 8 available go threads, but they don't get relinquished
;; even when the task is idle and awaitingg a blocking function.

;; One solution is to use async threads.
(defn launch-n-threads [n f]
  (let [<c (a/chan)]
    ;; producer
    (dotimes [i n]
      (a/thread (f) (a/>!! <c i)))
    ;; consumer
    (loop [i 0]
      (when-not (= i n) (a/<!! <c) (recur (inc i))))))

;; Now run it for 8, with a simulated one second blocking function.
(time (launch-n-threads 8 #(Thread/sleep 1000))) ;; 1005ms

;; Now run it for 9, with a simulated one second blocking function.
(time (launch-n-threads 9 #(Thread/sleep 1000))) ;; 1001ms

;; Now run it for 16, with a simulated one second blocking function.
(time (launch-n-threads 16 #(Thread/sleep 1000))) ;; 1002ms

;; Now run it for 17, with a simulated one second blocking function.
(time (launch-n-threads 17 #(Thread/sleep 1000))) ;; 1005ms


;; The above can be summaried with the use of the pipleine feature.
(defn pipeline
  [ n {:keys [blocking? parallelism]}]
  (let [<in (a/chan)
        <out (a/chan)
        task (fn [i] (Thread/sleep 1000) i)]
    (a/onto-chan <in (range n))
    ((if blocking? a/pipeline-blocking a/pipeline) parallelism <out (map task) <in)
    (loop [i 0]
      (when-not (= i n) (a/<!! <out) (recur (inc i))))))

;; We set the amount of items to process to 8, and parallelism to 8.
(time (pipeline 8 {:blocking? false :parallelism 8})) ;; 1011msec

;; Now we set the amount of items to process to 9, and parallelism to 8.
(time (pipeline 9 {:blocking? false :parallelism 8})) ;; 2004msec

;; Now we set the amount of items to process to 15, and parallelism to 8.
(time (pipeline 16 {:blocking? false :parallelism 8})) ;; 2003msec

;; So we see the same behaviour as launching n go blocks. But we have control
;; over the parallelims. So let's tweak it.
(time (pipeline 16 {:blocking? false :parallelism 16})) ;; 2005msec

;; It's still not increasing. This is because we've increased to 16 go blocks,
;; but there still use a pool of 8 threads under the hood. Let's tru blocking pipleines.
(time (pipeline 8 {:blocking? true :parallelism 8})) ;; 1003msec

;; We shouldn't see better performance if the parallelism stays down.
(time (pipeline 9 {:blocking? true :parallelism 8})) ;; 2004msec

;; But let's try to push it a bit.
(time (pipeline 9 {:blocking? true :parallelism 16})) ;; 1005msec

;; We expect the parallelism to be the upper bound here
(time (pipeline 160 {:blocking? true :parallelism 16})) ;; 10013msec

;; With non blocking, the number 8 is the upper bound, not 16.
(time (pipeline 160 {:blocking? false :parallelism 16})) ;; 20057msec

;; Let's push the limits a little with blocking functions.
(time (pipeline 320 {:blocking? true :parallelism 32})) ;; 10013msec

;; A little more.
(time (pipeline 1280 {:blocking? true :parallelism 128})) ;; 10065msec

;; We break things if we exceed the channel buffer of 1024
(time (pipeline 1025 {:blocking? true :parallelism 1025}))
