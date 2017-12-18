(ns mee6.store
  (:require [beicon.core :as rx]
            [potok.core :as ptk]))

(defonce state (atom {}))
(defonce store (ptk/store))

(defn emit!
  ([event]
   (ptk/emit! store event))
  ([event & events]
   (apply ptk/emit! store (cons event events))))

(defn- initial-state
  []
  {})

(defn init
  []
  (emit! initial-state)
  (rx/to-atom store state))
