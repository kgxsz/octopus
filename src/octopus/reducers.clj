(ns octopus.reducers
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

; For parralelized execution, use r/fold, with a foldable collection (maps or vectors.)
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

; To realize the collection in a parralell manner, call r/foldcat.
(->> [1 2 3 4]
     (r/map inc)
     (r/foldcat))

; To realize the collection in a serial manner, call clojure.core/into.
(->> [1 2 3 4]
     (r/map inc)
     (into []))


;; Performance

; First we define a non-foldable collection.
(def s (range 10000000))

; Then we get a baseline time for seq functions.
(time (->> s (filter even?) (map inc) (reduce +))) ; 531ms

; Then we use reducers, but without parralelization.
(time (->> s (r/filter even?) (r/map inc) (r/reduce +))) ; 382ms

; Then we use reducers with parralelization, but since the collection
; is non-foldable, the example below degenrates to the example above.
(time (->> s (r/filter even?) (r/map inc) (r/fold +))) ; 385ms

; Next we define a foldable collection.
(def v (vec s))

 ; Once more, we get a baseline time for seq functions.
(time (->> v (filter even?) (map inc) (reduce +))) ; 560ms

; Then we use reducers, but without parralelization.
(time (->> v (r/filter even?) (r/map inc) (r/reduce +))) ; 396ms

; Then we use reducers with parralelization.
(time (->> v (r/filter even?) (r/map inc) (r/fold +))) ; 286ms
