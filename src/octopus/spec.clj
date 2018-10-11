(ns octopus.spec
  (:require [clojure.spec.gen.alpha :as spec.gen]
            [clojure.spec.alpha :as spec]
            [clojure.spec.test.alpha :as spec.test]
            [expound.alpha :as expound]))


;; Generators

; First, obtain a generator from a spec.
(def some-generator (spec/gen #{:a :b :c}))

; Then generate a single value.
(spec.gen/generate some-generator)

; Or generate multiple values.
(spec.gen/sample some-generator)

; For conformed values, exercise is useful
(def some-other-generator (spec/or :int int? :string string?))

(spec/exercise some-other-generator 10)


;; Custom Generators

; Start with a spec for keywords with a particular namespace.
(spec/def ::my-namespaced-keyword (spec/and keyword? #(= (namespace %) "my.domain")))

; The spec works.
(spec/valid? ::my-namespaced-keyword :my.domain/name)

; You can explain the validity
(spec/explain ::my-namespaced-keyword :my.domain/name)
(spec/explain-str ::my-namespaced-keyword :my.domain/name)

; You can explain the invalidity
(spec/explain ::my-namespaced-keyword :your.domain/name)
(spec/explain-str ::my-namespaced-keyword :your.domain/name)

; The generator is unlikely to generate a significant match.
(spec.gen/sample (spec/gen ::my-namespaced-keyword))

; So you can generate from a more narrow generator.
(spec.gen/sample (spec/gen #{:my.domain/name :my.domain/occupation :my.domain/id}))

; You may redefine the original spec in terms of the narrower generator.
(spec/def ::my-namespaced-keyword
  (spec/with-gen
    (spec/and keyword? #(= (namespace %) "my.domain"))
    #(spec/gen #{:my.domain/name :my.domain/occupation :my.domain/id})))

; The spec still works.
(spec/valid? ::my-namespaced-keyword :my.domain/name)

; Now you can generate samples.
(spec.gen/sample (spec/gen ::my-namespaced-keyword))

; But we've lost flexibility here, by enumerating the generated output, we've constrained ourselves.
; We can build up guided generators with fmap.
(def my-namespaced-keyword-generator (spec.gen/fmap #(keyword "my.domain" %)
                                                    (spec.gen/string-alphanumeric)))

; It works as expected.
(spec.gen/sample my-namespaced-keyword-generator 5)

; Putting it all together.
(spec/def ::hello
  (spec/with-gen #(clojure.string/includes? % "hello")
    #(spec.gen/fmap
      (fn [[s1 s2]] (str s1 "hello" s2))
      (spec.gen/tuple (spec.gen/string-alphanumeric) (spec.gen/string-alphanumeric)))))

(spec.gen/sample (spec/gen ::hello))


;; Ranged Generators

; You can define ranges.
(spec/def ::dice-face (spec/int-in 1 7))

; Works as expected
(spec.gen/sample (spec/gen ::dice-face))


;; Intsrumentation and Testing

; Let's start with a function.
(defn ranged-rand
  "Returns random int in range start <= rand < end"
  [start end]
  (+ start (long (rand (- end start)))))

; We then spec it.
(spec/fdef
 ranged-rand
 :args (spec/and (spec/cat :start int? :end int?)
                 #(< (:start %) (:end %)))
 :ret int?
 :fn (spec/and #(>= (:ret %) (-> % :args :start))
               #(< (:ret %) (-> % :args :end))))

; Then we instrument it.
(spec.test/instrument `ranged-rand)

; Because it is instrumented, the args condition is checked, but not the others,
; because validating the implementation should be done at testing time.
(ranged-rand 5 3)

; We can check that the ret and fn parts of the fdef are satisfied with a check
; on the function with many different samples of args.
(spec.test/check `ranged-rand)


;; Expound

; Expound gives us nicer failure descriptions.
(expound/expound string? 1)

; Instead of calling spec's explain function, use expound's.
(expound/expound ::my-namespaced-keyword :my.domain/thing)
(expound/expound ::my-namespaced-keyword :your.domain/thing)

; Instead of calling spec's explain-str function, use expound's.
(expound/expound-str ::my-namespaced-keyword :my.domain/thing)
(expound/expound-str ::my-namespaced-keyword :your.domain/thing)

; It's easier if you simply set spec's explain-out var,
; this can be done dynamically, or for the current thread.
(spec/check-asserts true)
(binding [spec/*explain-out* expound/printer]
  (spec/assert ::my-namespaced-keyword :your.domain/thing))
(alter-var-root #'spec/*explain-out* (constantly expound/printer))

; To nicely format a test check, use explain-results
(expound/explain-results (spec.test/check `ranged-rand))

; You can also add nice error messages, instead of having the predicate printed
(expound/defmsg ::my-namespaced-keyword "Should be a keyword with a namespace of my.domain")
(expound/expound ::my-namespaced-keyword :your.domain/thing)

