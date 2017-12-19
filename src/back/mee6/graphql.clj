(ns mee6.graphql
  (:require [clojure.java.io :as io]
            [cuerdas.core :as str]
            [cheshire.core :as json]
            [com.walmartlabs.lacinia :as gql]
            [com.walmartlabs.lacinia.util :as gql-util :refer [attach-resolvers]]
            [com.walmartlabs.lacinia.schema :as gql-schema]
            [yaml.core :as yaml]
            [mee6.config :as cfg]
            [mee6.database :as db]
            [mee6.engine :as eng]
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
                            :resolve :get-host}
                     :module {:type :String}
                     :params {:type :dynobj :resolve :get-params}
                     :status {:type :String :resolve :get-status}
                     :output {:type :String
                              :args {:format {:type :String
                                              :default-value "json"}}
                              :resolve :get-output}
                     :error {:type :dynobj :resolve :get-error}
                     :updatedAt {:type :String
                                 :resolve :get-updated-at}
                     :config {:type :String
                              :args {:format {:type :String
                                              :default-value "yaml"}}
                              :resolve :get-config}}}}
   :queries
   {:checks {:type '(list :check)
             :resolve :get-checks}}

   :mutations
   {:login {:type :ID
            :args {:username {:type '(non-null :String)}
                   :password {:type '(non-null :String)}}
            :resolve :mutation-login}}})

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
      "yaml" (yaml/generate-string output :dumper-options {:flow-style :block})
      "json" (json/encode output))))

(defn resolve-error
  [ctx args {:keys [id]}]
  (get-in @db/state [:checks id :error]))

(defn resolve-config
  [ctx {:keys [format]} value]
  (case format
    "yaml" (yaml/generate-string value :dumper-options {:flow-style :block})
    "json" (json/encode value)))

(defn resolve-updated-at
  [ctx args {:keys [id]}]
  (get-in @db/state [:checks id :updated-at]))

;; --- Mutations

(defn- resolve-login
  [{:keys [rsp]} {:keys [username password]} value]
  (if-let [username (get-in cfg/config [:http :users (keyword username)])]
    (let [token (crypto/random-token)]
      (swap! db/state update :tokens assoc token (dt/now))
      (swap! rsp assoc :cookies {:auth-token {:value token :same-site :lax}})
      token)
    (throw (ex-info "Invalid credentials" {:type :wrong-credentials}))))

(def ^:private resolvers
  {:get-checks resolve-checks
   :get-host resolve-host
   :get-params resolve-params
   :get-status resolve-status
   :get-output resolve-output
   :get-error resolve-error
   :get-config resolve-config
   :get-updated-at resolve-updated-at
   :mutation-login resolve-login})

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
