(ns octopus.scratch)

(defn pipeline
  [ n {:keys [blocking? parallelism]}]
  (let [<in (a/chan)
        <out (a/chan)
        task (fn [i] (Thread/sleep 1000) i)]
    (a/onto-chan <in (range n))
    ((if blocking? a/pipeline-blocking a/pipeline) parallelism <out (map task) <in)
    (loop [i 0]
      (when-not (= i n) (a/<!! <out) (recur (inc i))))))


(defn make-process
  [process handle-interrupt handle-exception handle-process-end {:keys [hang-forever]}]
  (Thread.
   #(try
      (process)
      (when hang-forever
        (deref (promise)))
      (catch InterruptedException e
        (handle-interrupt e))
      (catch Exception e
        (handle-exception e))
      (finally
        (handle-process-end)))))


(defn make-load [config]
  (comp
   (map inc)
   (filter (:f config))))


(defn component [{:keys [config notifier upstream-component]}]
  (let [<input       (:<input upstream-component)
        <output      (a/chan)
        load         (make-load config)
        load-process (make-process
                      (a/pipeline 64 <output load <input)
                      #(println "load process interupted")
                      #(notify notifier :load-process %)
                      #()
                      )]
    )
  )
