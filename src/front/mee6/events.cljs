(ns mee6.events
  (:require [potok.core :as ptk]
            [beicon.core :as rx]
            [mee6.graphql :as gql]
            [mee6.store :as st]
            [mee6.util.router :as rt]))

;; --- Helpers

(defn- index-by
  [coll getter]
  (reduce #(assoc %1 (getter %2) %2) {} coll))

;; --- Auth Events

(defrecord LoggedIn [data]
  ptk/UpdateEvent
  (update [_ state]
    (assoc state :token (:login data)))

  ptk/WatchEvent
  (watch [_ state stream]
    (rx/of (rt/navigate :home))))

(def ^:private +login-query+
  "mutation Login($username: String!, $password: String!) {
     login(username: $username, password: $password)
   }")

(defrecord Login [params on-error]
  ptk/WatchEvent
  (watch [_ state stream]
    (letfn [(handle-error [{:keys [type] :as error}]
              (if (= type "wrong-credentials")
                (do
                  (on-error error)
                  (rx/empty))
                (rx/throw error)))]
      (->> (gql/query +login-query+ params)
           (rx/catch handle-error)
           (rx/map ->LoggedIn)))))

(defrecord LoggedOut []
  ptk/UpdateEvent
  (update [_ state]
    (dissoc state :token))

  ptk/WatchEvent
  (watch [_ state stream]
    (rx/of (rt/navigate :login))))

(defrecord Logout []
  ptk/WatchEvent
  (watch [_ state stream]
    (->> (gql/query "mutation Logout {logout}")
         (rx/map ->LoggedOut))))

;; --- Checks Events

(defrecord ChecksRetrieved [checks]
  ptk/UpdateEvent
  (update [_ state]
    (->> (index-by checks :id)
         (assoc state :checks))))

(def ^:private +checks-query+
  "query RetrieveCheccks($outputFormat: String!) {
     checks {
       id, name, host, cron, status,
       tags, config, error, updatedAt
       output(format: $outputFormat)
     }
   }")

(defrecord RetrieveChecks []
  ptk/WatchEvent
  (watch [_ state stream]
    (->> (gql/query +checks-query+ {:outputFormat "yaml"})
         (rx/map :checks)
         (rx/map ->ChecksRetrieved))))

(defrecord UpdateSelectedTag [tag]
  ptk/UpdateEvent
  (update [_ {:keys [selected-tag] :as state}]
    (if (= tag selected-tag)
      (dissoc state :selected-tag)
      (assoc state :selected-tag tag))))
