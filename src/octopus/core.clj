(ns octopus.core
  (:require [clojure.core.reducers :as r]))


;; Reducers vs. Seq Functions

; The seq version.
(->> (range 10)
     (map inc)
     (filter even?)
     (reduce +))

; The reducer version.
(->> (range 10)
     (r/map inc)
     (r/filter even?)
     (r/reduce +))


;; How to use Reducers

; Reducers are eager, eliminate intermediate collections, and parralelize the work.
; The example below eliminates intermediate collections, but is not parralelized.

(->> [1 2 3 4]
     (r/filter even?)
     (r/map inc)
     (r/reduce +))

; For parralelized execution, use fold, with a foldable collection (maps or vectors.)

(->> [1 2 3 4]
     (r/filter even?)
     (r/map inc)
     (r/fold +))

; If you use fold on a non foldable collection, it will silently fallback to r/reduce

(->> '(1 2 3 4)
     (r/filter even?)
     (r/map inc)
     (r/fold +))

; Reducer functions return reducibles, not collections.
; To realize the collection, call r/foldcat or clojure.core/into on the reducible

(->> [1 2 3 4]
     (r/map inc)
     (r/foldcat))

(->> [1 2 3 4]
     (r/map inc)
     (into []))
