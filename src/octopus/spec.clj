(ns octopus.spec
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))


;; Generators

; First, obtain a generator from a spec.
(def some-generator (s/gen #{:a :b :c}))

; Then generate a single value.
(gen/generate some-generator)

; Or generate multiple values.
(gen/sample some-generator)

; For conformed values, exercise is useful
(def some-other-generator (s/or :int int? :string string?))

(s/exercise some-other-generator 10)


;; Custom Generators

; Start with a spec for keywords with a particular namespace.
(s/def ::my-namespaced-keyword (s/and keyword? #(= (namespace %) "my.domain")))

; The spec works.
(s/valid? ::my-namespaced-keyword :my.domain/name)

; But it's unlikely to generate a significant match.
(gen/sample (s/gen ::my-namespaced-keyword))

; So you can generate from a more narrow generator.
(gen/sample (s/gen #{:my.domain/name :my.domain/occupation :my.domain/id}))

; You may redefine the original spec in terms of the narrower generator.
(s/def ::my-namespaced-keyword
  (s/with-gen (s/and keyword? #(= (namespace %) "my.domain"))
    #(s/gen #{:my.domain/name :my.domain/occupation :my.domain/id})))

; The spec still works.
(s/valid? ::my-namespaced-keyword :my.domain/name)

; Now you can generate samples.
(gen/sample (s/gen ::my-namespaced-keyword))

;; But we've lost flexibility here, by enumerating the generated output, we've constrained ourselves.
;; We can build up guided generators with fmap.
(def my-namespaced-keyword-generator (gen/fmap #(keyword "my.domain" %)
                                               (gen/string-alphanumeric)))

;; It works as expected.
(gen/sample my-namespaced-keyword-generator 5)

;; Putting it all together.
(s/def ::hello
  (s/with-gen #(clojure.string/includes? % "hello")
    #(gen/fmap (fn [[s1 s2]] (str s1 "hello" s2))
               (gen/tuple (gen/string-alphanumeric) (gen/string-alphanumeric)))))

(gen/sample (s/gen ::hello))
