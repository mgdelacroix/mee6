(ns mee6.http.graphql
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia :refer [execute]]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
            [com.walmartlabs.lacinia.schema :as schema]
            [cuerdas.core :as str]
            [cheshire.core :as json]
            [mee6.template :as tmpl]))

(def identity-conformer (schema/as-conformer identity))

(def schema
  {:scalars {:dynobj {:parse identity-conformer
                      :serialize identity-conformer}}
   :objects
   {:check {:fields {:id {:type :ID}
                     :name {:type :String}
                     :cron {:type :String}
                     :host {:type :String}
                     :params {:type :dynobj}}}}
   :queries
   {:checks {:type '(list :check)
             :resolve :get-checks}}})

(defn checks-resolver
  [ctx args value]
  (println "- Resolver:")
  (println "| ctx" ctx)
  (println "| args" args)
  (println "| value" value)
  [])

(def resolvers
  {:get-checks checks-resolver})

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
