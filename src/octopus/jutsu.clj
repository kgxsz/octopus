(ns octopus.jutsu
  (:require [jutsu.core :as jutsu]))

(jutsu/start-jutsu!)

(jutsu/graph!
 "Something"
 [{:x [1 2 3 4]
   :y [1 2 3 4]
   :mode "markers"
   :type "scatter"}])
