(ns mee6.graphql
  (:require [clojure.java.io :as io]
            [cuerdas.core :as str]
            [cheshire.core :as json]
            [com.walmartlabs.lacinia :as gql]
            [com.walmartlabs.lacinia.util :as gql-util :refer [attach-resolvers]]
            [com.walmartlabs.lacinia.schema :as gql-schema]
            [mee6.config :as cfg]
            [mee6.database :as db]
            [mee6.engine :as eng]
            [mee6.util.yaml :as yaml]
            [mee6.util.time :as dt]
            [mee6.util.crypto :as crypto]))

(def ^:private identity-conformer
  (gql-schema/as-conformer identity))

(def ^:private schema-data
  {:scalars
   {:dynobj {:parse identity-conformer
             :serialize identity-conformer}}
   :objects
   {:check {:fields {:id {:type :ID}
                     :name {:type :String}
                     :cron {:type :String}
                     :host {:type :String
                            :resolve :resolve-host}
                     :module {:type :String}
                     :params {:type :dynobj :resolve :resolve-params}
                     :status {:type :String :resolve :resolve-status}
                     :output {:type :String
                              :args {:format {:type :String
                                              :default-value "json"}}
                              :resolve :resolve-output}
                     :error {:type :dynobj :resolve :resolve-error}
                     :updatedAt {:type :String
                                 :resolve :resolve-updated-at}
                     :config {:type :String
                              :args {:format {:type :String
                                              :default-value "yaml"}}
                              :resolve :resolve-config}}}}
   :queries
   {:checks {:type '(list :check)
             :resolve :resolve-checks}}

   :mutations
   {:login {:type :ID
            :args {:username {:type '(non-null :String)}
                   :password {:type '(non-null :String)}}
            :resolve :mutation-login}
    :logout {:type :ID
             :resolve :mutation-logout}}})

;; --- Queries

(defn resolve-checks
  [ctx args value]
  (when-not (:authenticated ctx)
    (throw (ex-info "Not authenticated" {:type :not-authorized})))
  eng/checks)

(defn resolve-params
  [ctx args value]
  (apply dissoc value [:id :name :cron :host :module]))

(defn resolve-host
  [ctx args {:keys [host]}]
  (if (keyword? host)
    (name host)
    (:uri host)))

(defn resolve-status
  [ctx args {:keys [id]}]
  (some-> (get-in @db/state [:checks id :status])
          (name)))

(defn resolve-output
  [ctx {:keys [format]} {:keys [id]}]
  (let [output (get-in @db/state [:checks id :local])]
    (case format
      "yaml" (yaml/encode output)
      "json" (json/encode output))))

(defn resolve-error
  [ctx args {:keys [id]}]
  (get-in @db/state [:checks id :error]))

(defn resolve-config
  [ctx {:keys [format]} value]
  (case format
    "yaml" (yaml/encode value)
    "json" (json/encode value)))

(defn resolve-updated-at
  [ctx args {:keys [id]}]
  (get-in @db/state [:checks id :updated-at]))

;; --- Mutations

(defn- resolve-login
  [{:keys [rsp]} {:keys [username password]} value]
  (let [pwhash (get-in cfg/config [:http :users (keyword username)])]
    (when-not (crypto/verify-password pwhash password)
      (throw (ex-info "Invalid credentials" {:type :wrong-credentials})))
    (let [token (crypto/random-token)]
      (swap! db/state update :tokens assoc token (dt/now))
      (swap! rsp assoc :cookies {:auth-token {:value token :same-site :lax}})
      token)))

(defn- resolve-logout
  [{:keys [rsp] :as ctx} args value]
  (swap! rsp assoc :cookies {:auth-token {:value "" :same-site :lax :max-age -1}})
  true)

(def ^:private resolvers
  {:resolve-checks resolve-checks
   :resolve-host resolve-host
   :resolve-params resolve-params
   :resolve-status resolve-status
   :resolve-output resolve-output
   :resolve-error resolve-error
   :resolve-config resolve-config
   :resolve-updated-at resolve-updated-at
   :mutation-login resolve-login
   :mutation-logout resolve-logout})

(def ^:private schema
  (-> schema-data
      (gql-util/attach-resolvers resolvers)
      (gql-schema/compile)))

(defn execute
  [query params context]
  (try
    (gql/execute schema query params context)
    (catch Throwable e
      {:errors [(gql-util/as-error-map e)]})))
