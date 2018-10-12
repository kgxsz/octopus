(ns octopus.jutsu
  (:require [jutsu.core :as j]))


;;  Graphing

; Start Jutsu.
(j/start-jutsu!)

; Graph this thing in the browser.
(j/graph!
 "My First Graph"
 [{:x [1 2 3 4]
   :y [1 2 3 4]
   :mode "markers"
   :type "scatter"}])

; You can do real time updates.
(j/update-graph!
 "My First Graph"
 {:data {:y [[4]] :x [[5]]}
  :traces [0]})
