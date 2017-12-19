(ns mee6.events
  (:require [potok.core :as ptk]
            [beicon.core :as rx]
            [mee6.graphql :as gql]))

(defn- index-by
  [coll getter]
  (reduce #(assoc %1 (getter %2) %2) {} coll))

(defrecord ChecksRetrieved [checks]
  ptk/UpdateEvent
  (update [_ state]
    (->> (index-by checks :id)
         (assoc state :checks))))

(defrecord RetrieveChecks []
  ptk/WatchEvent
  (watch [_ state stream]
    (->> (gql/query "{checks {id name host cron status config output error updatedAt}}")
         (rx/map :checks)
         (rx/map ->ChecksRetrieved))))
