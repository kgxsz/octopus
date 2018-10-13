(ns octopus.daemons
  (:gen-class)
  (:require [clojure.core.async :as a]))


;; Daemons


;; First, let's define a Java thread.
(defn make-java-thread [f]
  (Thread.
   #(try
      (f)
      (catch InterruptedException e
        (println "shutting down the Java thread"))
      (finally
        (println "doing a final thing")))))

;; Let's also define a dumb counter.
(defn counter [count-name n]
  (doseq [i (reverse (range n))]
    (println count-name (inc i))
    (Thread/sleep 1000)))

;; If you do `lein with-profiles daemons run` with the following main function,
;; you will see that the thread survives even if the main function exits. This
;; is because it is a non daemon thread.
(comment
  (defn -main [& args]
    (let [counter-process (make-java-thread #(counter "secondary" 10))]
      (.start counter-process)
      (Thread/sleep 500)
      (counter "primary" 5))))

;; To stop a non daemon thread, we can interrupt it from the main function.
(comment
  (defn -main [& args]
    (let [counter-process (make-java-thread #(counter "secondary" 10))]
      (.start counter-process)
      (Thread/sleep 500)
      (counter "primary" 5)
      (.interrupt counter-process))))

;; We can use a future instead of a Java thread too, it will run the task
;; in a thread, the thread will also be non daemon, and survive the main function
;; exits. But unlike the Java thread, it will hang, even when its body is complete.
;; Even if it is dereferenced. There is no simple way to interrupt it from
;; within the main function.
(comment
  (defn -main [& args]
    (let [!counter-process (future (counter "secondary" 10))]
      (Thread/sleep 500)
      (counter "primary" 5)
      (deref !counter-process))))

;; We can also use an async thread, it will run the task but will not
;; survive if the main function exits. This is a daemon thread.
(comment
  (defn -main [& args]
    (a/thread (counter "secondary" 10))
    (Thread/sleep 500)
    (counter "primary" 5)))

;; Finally, we can use a go block, again, it will not survive if the main function exits.
;; This is also a daemon thread.
(comment
  (defn -main [& args]
    (a/go (counter "secondary" 10))
    (Thread/sleep 500)
    (counter "primary" 5)))
