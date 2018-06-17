(ns octopus.jutsu
  (:require [jutsu.core :as jutsu]
            [clj-time.core :as time]
            [clj-time.coerce :as time.coerce]
            [clj-time.format :as time.format]))

; 1525647600000 -- May 7th
; 1525734000000 -- May 8th
; 1528844400000 -- June 13th

(jutsu/start-jutsu!)

(defn read-instants [filename]
  (with-open [r (clojure.java.io/reader filename)]
    (mapv #(Long/parseLong %) (line-seq r))))

(defn read-dates [filename]
  (let [date-formatter (time.format/formatter :date)]
    (with-open [r (clojure.java.io/reader filename)]
      (mapv #(->> % (time.format/parse date-formatter) (time.coerce/to-long)) (line-seq r)))))

(defn normalize [xs]
  (->> xs
       (mapv #(- % 1525647600000)) ;; normalize to May 7th
       (remove neg?)
       (mapv #(/ % 86400000))))

(def lo2 (read-dates "/Users/ksuzukawa/Downloads/lo2"))

(def ld1 (read-instants "/Users/ksuzukawa/Downloads/ld1"))

(def lu1 (read-instants "/Users/ksuzukawa/Downloads/lu1"))

(def lc2 (read-instants "/Users/ksuzukawa/Downloads/lc2"))

(def rr2 (read-instants "/Users/ksuzukawa/Downloads/rr2"))

(def rr3 (read-instants "/Users/ksuzukawa/Downloads/rr3"))

(def lb1 (read-instants "/Users/ksuzukawa/Downloads/lb1"))

(jutsu/graph!
 "Timing"
 (into []
  (map-indexed
   (fn [i points]
     (let [normalized-points (normalize points)]
       {:x normalized-points
        :y (repeat (count normalized-points) i)
        :mode "markers"
        :type "scatter"}))
   [lb1 rr3 rr2 lc2 lu1 ld1 lo2])))
