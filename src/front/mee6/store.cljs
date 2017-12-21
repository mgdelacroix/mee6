(ns mee6.store
  (:require [beicon.core :as rx]
            [potok.core :as ptk])
  (:import [goog.net Cookies]))

(def ^:dynamic *on-error* identity)

(defonce state (atom {}))
(defonce store (ptk/store {:on-error #(*on-error* %)}))

(defn emit!
  ([event]
   (ptk/emit! store event))
  ([event & events]
   (apply ptk/emit! store (cons event events))))

(defn- initial-state
  []
  (let [cookies (Cookies. js/document)]
    {:token (.get cookies "auth-token")}))

(defn init
  []
  (emit! initial-state)
  (rx/to-atom store state))
