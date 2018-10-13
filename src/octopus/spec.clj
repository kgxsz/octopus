(ns octopus.spec
  (:require [clojure.spec.gen.alpha :as s.gen]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as s.test]
            [expound.alpha :as expound]))


;; Generators


;; First, obtain a generator from a spec.
(def some-generator (s/gen #{:a :b :c}))

;; Then generate a single value.
(s.gen/generate some-generator)

;; Or generate multiple values.
(s.gen/sample some-generator)

;; For conformed values, exercise is useful
(def some-other-generator (s/or :int int? :string string?))

(s/exercise some-other-generator 10)



;; Custom Generators


;; Start with a spec for keywords with a particular namespace.
(s/def ::my-namespaced-keyword (s/and keyword? #(= (namespace %) "my.domain")))

;; The spec works.
(s/valid? ::my-namespaced-keyword :my.domain/name)

;; You can explain the validity
(s/explain ::my-namespaced-keyword :my.domain/name)
(s/explain-str ::my-namespaced-keyword :my.domain/name)

;; You can explain the invalidity
(s/explain ::my-namespaced-keyword :your.domain/name)
(s/explain-str ::my-namespaced-keyword :your.domain/name)

;; The generator is unlikely to generate a significant match.
(s.gen/sample (s/gen ::my-namespaced-keyword))

;; So you can generate from a more narrow generator.
(s.gen/sample (s/gen #{:my.domain/name :my.domain/occupation :my.domain/id}))

;; You may redefine the original spec in terms of the narrower generator.
(s/def ::my-namespaced-keyword
  (s/with-gen
    (s/and keyword? #(= (namespace %) "my.domain"))
    #(s/gen #{:my.domain/name :my.domain/occupation :my.domain/id})))

;; The spec still works.
(s/valid? ::my-namespaced-keyword :my.domain/name)

;; Now you can generate samples.
(s.gen/sample (s/gen ::my-namespaced-keyword))

;; But we've lost flexibility here, by enumerating the generated output, we've constrained ourselves.
;; We can build up guided generators with fmap.
(def my-namespaced-keyword-generator (s.gen/fmap #(keyword "my.domain" %)
                                                    (s.gen/string-alphanumeric)))

;; It works as expected.
(s.gen/sample my-namespaced-keyword-generator 5)

;; Putting it all together.
(s/def ::hello
  (s/with-gen #(clojure.string/includes? % "hello")
    #(s.gen/fmap
      (fn [[s1 s2]] (str s1 "hello" s2))
      (s.gen/tuple (s.gen/string-alphanumeric) (s.gen/string-alphanumeric)))))

(s.gen/sample (s/gen ::hello))



;; Ranged Generators


;; You can define ranges.
(s/def ::dice-face (s/int-in 1 7))

;; Works as expected
(s.gen/sample (s/gen ::dice-face))


;;; Intsrumentation and Testing

;; Let's start with a function.
(defn ranged-rand
  "Returns random int in range start <= rand < end"
  [start end]
  (+ start (long (rand (- end start)))))

;; We then spec it.
(s/fdef
 ranged-rand
 :args (s/and (s/cat :start int? :end int?)
                 #(< (:start %) (:end %)))
 :ret int?
 :fn (s/and #(>= (:ret %) (-> % :args :start))
               #(< (:ret %) (-> % :args :end))))

;; Then we instrument it.
(s.test/instrument `ranged-rand)

;; Because it is instrumented, the args condition is checked, but not the others,
;; because validating the implementation should be done at testing time.
(ranged-rand 5 3)

;; We can check that the ret and fn parts of the fdef are satisfied with a check
;; on the function with many different samples of args.
(s.test/check `ranged-rand)



;; Expound


;; Expound gives us nicer failure descriptions.
(expound/expound string? 1)

;; Instead of calling spec's explain function, use expound's.
(expound/expound ::my-namespaced-keyword :my.domain/thing)
(expound/expound ::my-namespaced-keyword :your.domain/thing)

;; Instead of calling spec's explain-str function, use expound's.
(expound/expound-str ::my-namespaced-keyword :my.domain/thing)
(expound/expound-str ::my-namespaced-keyword :your.domain/thing)

;; It's easier if you simply set spec's explain-out var,
;; this can be done dynamically, or for the current thread.
(s/check-asserts true)
(binding [s/*explain-out* expound/printer]
  (s/assert ::my-namespaced-keyword :your.domain/thing))
(alter-var-root #'s/*explain-out* (constantly expound/printer))

;; To nicely format a test check, use explain-results
(expound/explain-results (s.test/check `ranged-rand))

;; You can also add nice error messages, instead of having the predicate printed
(expound/defmsg ::my-namespaced-keyword "Should be a keyword with a namespace of my.domain")
(expound/expound ::my-namespaced-keyword :your.domain/thing)
