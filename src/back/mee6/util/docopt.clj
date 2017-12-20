(ns mee6.util.docopt
  (:require [docopt.core :as dopt]
            [docopt.match :as doptm]))

(defn parse
  "Parse command line and return a map with matched params."
  [doc args]
  (-> (dopt/parse doc)
      (doptm/match-argv args)))
