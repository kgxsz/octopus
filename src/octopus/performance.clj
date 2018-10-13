(ns octopus.performance
  (:gen-class)
  (:require [clojure.core.async :as a]))


;; Non Daemon Threads

; First, let's define a Java thread.
(defn make-java-thread [f]
  (Thread.
   #(try
      (f)
      (catch InterruptedException e
        (println "shutting down the Java thread"))
      (finally
        (println "doing a final thing")))))

; Then a shutdown hook to stop any non daemon threads
(defn add-shutdown-hook [process]
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. #(.interrupt process))))

; Let's also define a dumb counter.
(defn counter [count-name n]
  (doseq [i (reverse (range n))]
    (println count-name (inc i))
    (Thread/sleep 1000)))

; If you do `lein with-profiles performance run` with the following main function,
; you will see that the thread survives even if the main function exits.
(defn -main [& args]
  (let [counter-process (make-java-thread #(counter "secondary" 10))]
    (.start counter-process)
    (Thread/sleep 500)
    (counter "primary" 5)))

; We can also interrupt the thread from the main function.
(comment
  (defn -main [& args]
    (let [counter-process (make-java-thread #(counter "secondary" 10))]
      (.start counter-process)
      (Thread/sleep 500)
      (counter "primary" 5)
      (.interrupt counter-process))))

; We can use a future instead of a Java thread too, it will run the task
; in a thread, the thread will survive the main function exits and
; the task is complete. It will hang even if future is dereferenced. There is
; no simple way to interrupt it from within the main function.
(comment
  (defn -main [& args]
    (let [!counter-process (future (counter "secondary" 10))]
      (Thread/sleep 500)
      (counter "primary" 5)
      (deref !counter-process))))

; We can also use an async thread, it will run the task but will not
; survive if the main function exits.
(comment
  (defn -main [& args]
    (a/thread (counter "secondary" 10))
    (Thread/sleep 500)
    (counter "primary" 5)))

; Finally, we can use a go block, again, it will not survive if the main function exits.
(defn -main [& args]
  (a/go (counter "secondary" 10))
  (Thread/sleep 500)
  (counter "primary" 5))


; Now let's define a function that launches n go blocks and runs a function f in each go block.
(defn launch-n-go-blocks [n f]
  (let [c (a/chan)]
    ;; producer
    (dotimes [i n]
      (a/go (f) (a/>! c i)))
    ;; consumer
    (loop [i 0]
      (when-not (= i n) (a/<!! c) (recur (inc i))))))

; Now run it for 8, with a simulated one second blocking function.
(time (launch-n-go-blocks 8 #(Thread/sleep 1000))) ; 1005ms

; Now run it for 9, with a simulated one second blocking function.
(time (launch-n-go-blocks 9 #(Thread/sleep 1000))) ; 2004ms

; Now run it for 16, with a simulated one second blocking function.
(time (launch-n-go-blocks 16 #(Thread/sleep 1000))) ; 2010ms

; Now run it for 17, with a simulated one second blocking function.
(time (launch-n-go-blocks 17 #(Thread/sleep 1000))) ; 3008ms

; clearly, there are 8 available go threads, but they don't get relinquished
; even when the task is idle and awaitingg a blocking function.

; One solution is to use async threads.
(defn launch-n-threads [n f]
  (let [c (a/chan)]
    ;; producer
    (dotimes [i n]
      (a/thread (f) (a/>!! c i)))
    ;; consumer
    (loop [i 0]
      (when-not (= i n) (a/<!! c) (recur (inc i))))))

; Now run it for 8, with a simulated one second blocking function.
(time (launch-n-threads 8 #(Thread/sleep 1000))) ; 1005ms

; Now run it for 9, with a simulated one second blocking function.
(time (launch-n-threads 9 #(Thread/sleep 1000))) ; 1001ms

; Now run it for 16, with a simulated one second blocking function.
(time (launch-n-threads 16 #(Thread/sleep 1000))) ; 1002ms

; Now run it for 17, with a simulated one second blocking function.
(time (launch-n-threads 17 #(Thread/sleep 1000))) ; 1005ms

; But threads are heavy, so what if we used non-blocking functions.
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

