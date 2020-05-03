(ns octopus.clara
  (:require [clara.rules :refer :all]))


(clear-ns-productions!)

(defrule some-rule
  [:some-fact]
  =>
  (println "insert some other fact")
  (insert! {:fact-type :some-other-fact}))

(defn make-decision
  []
  (-> (mk-session 'octopus.clara
                  :fact-type-fn :fact-type
                  :cache false)
      (insert {:fact-type :some-fact})
      (fire-rules)))

(make-decision)
