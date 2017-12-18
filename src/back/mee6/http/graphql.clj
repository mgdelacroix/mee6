(ns mee6.http.graphql
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia :refer [execute]]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
            [com.walmartlabs.lacinia.schema :as schema]
            [cuerdas.core :as str]
            [cheshire.core :as json]
            [yaml.core :as yaml]
            [mee6.template :as tmpl]
            [mee6.engine :as eng]
            [mee6.database :as db]))

(def identity-conformer (schema/as-conformer identity))

(def schema
  {:scalars {:dynobj {:parse identity-conformer
                      :serialize identity-conformer}}
   :objects
   {:check {:fields {:id {:type :ID}
                     :name {:type :String}
                     :cron {:type :String}
                     :host {:type :String}
                     :module {:type :String}
                     :params {:type :dynobj
                              :resolve :get-params}
                     :status {:type :String
                              :resolve :get-status}
                     :output {:type :dynobj
                              :resolve :get-output}
                     :error {:type :dynobj
                             :resolve :get-error}
                     :config {:type :String
                              :args {:format {:type :String
                                              :default-value "yaml"}}
                              :resolve :get-config}}}}
   :queries
   {:checks {:type '(list :check)
             :resolve :get-checks}}})

(defn resolve-checks
  [ctx args value]
  eng/checks)

(defn resolve-params
  [ctx args value]
  (apply dissoc value [:id :name :cron :host :module]))

(defn resolve-status
  [ctx args {:keys [id]}]
  (some-> (get-in @db/state [:checks id :status])
          (name)))

(defn resolve-output
  [ctx args {:keys [id]}]
  (get-in @db/state [:checks id :local]))

(defn resolve-error
  [ctx args {:keys [id]}]
  (get-in @db/state [:checks id :error]))

(defn resolve-config
  [ctx {:keys [format]} value]
  (case format
    "yaml" (yaml/generate-string value :dumper-options {:flow-style :block})
    "json" (json/encode value)))

(def resolvers
  {:get-checks resolve-checks
   :get-params resolve-params
   :get-status resolve-status
   :get-output resolve-output
   :get-error resolve-error
   :get-config resolve-config})

(def compiled-schema
  (-> schema
      (attach-resolvers resolvers)
      (schema/compile)))

;; --- Handlers

(defn serialize
  [v]
  (-> (json/encode v)
      (str/replace #"\/" "\\/")))

(defn handle-graphiql
  [req]
  (let [ctx {:query (serialize (get-in req [:params "query"]))
             :variables (serialize (get-in req [:params "variables"]))
             :operation (serialize (get-in req [:params "operationName"]))}
        body (tmpl/render "graphiql.html" ctx)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body body}))

(defn handle-graphql
  [req]
  (let [query (get-in req [:body :query])
        variables (get-in req [:body :variables])
        result (execute compiled-schema query variables nil)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/encode result)}))
