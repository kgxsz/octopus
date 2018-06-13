(ns octopus.jutsu
  (:require [jutsu.core :as jutsu]
            [clj-time.core :as time]
            [clj-time.coerce :as time.coerce]
            [clj-time.format :as time.format]))

; 1525734000000 -- May 8th
; 1528844400000 -- June 13th

(jutsu/start-jutsu!)

(def instants
  (with-open [r (clojure.java.io/reader "/Users/ksuzukawa/Downloads/rr2-pub")]
    (mapv #(Long/parseLong %) (line-seq r))))

(def dates
  (let [date-formatter (time.format/formatter :date)]
    (with-open [r (clojure.java.io/reader "/Users/ksuzukawa/Downloads/rr2-eff")]
      (mapv #(->> % (time.format/parse date-formatter) (time.coerce/to-long)) (line-seq r)))))

(jutsu/graph!
 "rr2"
 [{:x instants
   :y (map #(-> %1 (- %2) (/ 86400000) int) instants dates)
   :mode "markers"
   :type "scatter"}])
