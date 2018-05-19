(ns octopus.threadpools
  (:import [java.util.concurrent Executors TimeUnit]))


;; Threadpools

; we create a threadpool
(def cached-thread-pool (Executors/newCachedThreadPool))

; we have zero active threads
(.getActiveCount cached-thread-pool)

; submit a dumb function to the threadpool
(def keep-running (atom true))
(.submit cached-thread-pool (fn [] (while @keep-running)))

; we have one active thread
(.getActiveCount cached-thread-pool)

; see how long idle threads are kept around
(.getKeepAliveTime cached-thread-pool TimeUnit/MINUTES)

; stop the dumb function from looping
(swap! keep-running not)

; we have zero active threads
(.getActiveCount cached-thread-pool)

; we have one completed task
(.getCompletedTaskCount cached-thread-pool)

; we may have one thread idle
(.getPoolSize cached-thread-pool)
