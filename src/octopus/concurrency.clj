(ns octopus.concurrency
  (:require [clojure.core.async :as a]))


;; Concurrency


;; Now let's define a function that launches n go blocks and runs a function f in each go block.
(defn launch-n-go-blocks [n f]
  (let [c (a/chan)]
    ;; producer
    (dotimes [i n]
      (a/go (f) (a/>! c i)))
    ;; consumer
    (loop [i 0]
      (when-not (= i n) (a/<!! c) (recur (inc i))))))

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
  (let [c (a/chan)]
    ;;; producer
    (dotimes [i n]
      (a/thread (f) (a/>!! c i)))
    ;;; consumer
    (loop [i 0]
      (when-not (= i n) (a/<!! c) (recur (inc i))))))

;; Now run it for 8, with a simulated one second blocking function.
(time (launch-n-threads 8 #(Thread/sleep 1000))) ;; 1005ms

;; Now run it for 9, with a simulated one second blocking function.
(time (launch-n-threads 9 #(Thread/sleep 1000))) ;; 1001ms

;; Now run it for 16, with a simulated one second blocking function.
(time (launch-n-threads 16 #(Thread/sleep 1000))) ;; 1002ms

;; Now run it for 17, with a simulated one second blocking function.
(time (launch-n-threads 17 #(Thread/sleep 1000))) ;; 1005ms

;; But threads are heavy, so what if we used non-blocking functions.
(defn launch-n-non-blocking-go-blocks [n]
  (let [c (a/chan)]
    ;; producer
    (doseq [!result (repeatedly n promise)]
      (a/go
        (a/>! c !result)
        (future (do (Thread/sleep 1000) (deliver !result :result)))))
    ;; consumer
    (loop [promises []]
      (if (= n (count promises))
        (mapv deref promises)
        (recur (conj promises (a/<!! c)))))))

(time (launch-n-non-blocking-go-blocks 1000))

(time (launch-n-threads 1000 #(Thread/sleep 1000)))

