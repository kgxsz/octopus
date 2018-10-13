(ns octopus.threadpools
  (:import [java.util.concurrent Executors TimeUnit]))


;; Threadpools


;; We create a threadpool.
(def cached-thread-pool (Executors/newCachedThreadPool))

;; We have zero active threads.
(.getActiveCount cached-thread-pool)

;; Submit a dumb function to the threadpool.
(def keep-running (atom true))
(.submit cached-thread-pool (fn [] (while @keep-running)))

;; We have one active thread.
(.getActiveCount cached-thread-pool)

;; See how long idle threads are kept around.
(.getKeepAliveTime cached-thread-pool TimeUnit/MINUTES)

;; Stop the dumb function from looping.
(swap! keep-running not)

;; We have zero active threads.
(.getActiveCount cached-thread-pool)

;; We have one completed task.
(.getCompletedTaskCount cached-thread-pool)

;; We may have one thread idle.
(.getPoolSize cached-thread-pool)
