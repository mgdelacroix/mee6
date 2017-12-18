(ns mee6.main
  (:require [mee6.store :as st]
            [mee6.ui :as ui]))

(enable-console-print!)

(defn ^:export init
  []
  (st/init)
  (ui/init))
