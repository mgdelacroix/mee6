(ns hal.core
  (:require [hal.config :as cfg]))

(defn -main
  [& [path]]
  (cfg/parse path))

