(ns mee6.main
  (:require [mee6.store :as st]))

(enable-console-print!)

(println "Hello Mee6!")

(defn ^:export init
  []
  (st/init))
