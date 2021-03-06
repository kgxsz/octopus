(defproject octopus "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [com.cerner/clara-rules "0.20.0"]
                 [hswick/jutsu "0.1.2"]
                 [expound "0.7.1"]]

  :profiles {:daemons {:main ^:skip-aot octopus.daemons}})
