(ns mee6.http.graphql
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia :refer [execute]]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
            [com.walmartlabs.lacinia.schema :as schema]
            [cheshire.core :as json]))

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

(defn graphiql-handler
  [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp (io/resource "graphiql.html"))})

(defn handler
  [req]
  (let [query (get-in req [:query-params :q])
        result (execute compiled-schema query nil nil)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/encode result)}))
