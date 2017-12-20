(ns mee6.events
  (:require [potok.core :as ptk]
            [beicon.core :as rx]
            [mee6.graphql :as gql]
            [mee6.util.router :as rt]
            [mee6.store :as st]))

(defn- index-by
  [coll getter]
  (reduce #(assoc %1 (getter %2) %2) {} coll))

(defrecord LoginRetrieved [data]
  ptk/UpdateEvent
  (update [_ state]
    (assoc state :token (:login data)))
  ptk/WatchEvent
  (watch [_ state stream]
    (rx/of (rt/navigate :home))))

(defrecord RetrieveLogin [username password]
  ptk/WatchEvent
  (watch [_ state stream]
    (let [text (str "mutation Login($username: String!, $password: String!) {
                       login(username: $username, password: $password)
                     }")
          params {:username username :password password}]
      (->> (gql/query text params)
           (rx/map ->LoginRetrieved)))))

(defrecord RetrieveLogout []
  ptk/WatchEvent
  (watch [_ state stream]
    (gql/query "mutation Logout {logout}")))

(defrecord ChecksRetrieved [checks]
  ptk/UpdateEvent
  (update [_ state]
    (->> (index-by checks :id)
         (assoc state :checks))))

(defrecord RetrieveChecks []
  ptk/WatchEvent
  (watch [_ state stream]
    (->> (gql/query "{checks {id name host cron status config output(format: \"yaml\") error updatedAt}}")
         (rx/map :checks)
         (rx/map ->ChecksRetrieved))))
