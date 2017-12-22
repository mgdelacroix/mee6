(ns mee6.util.graphql
  (:refer-clojure :exclude [compile])
  (:require [clojure.stacktrace :as st]
            [com.walmartlabs.lacinia :as gql]
            [com.walmartlabs.lacinia.util :as gql-util ]
            [com.walmartlabs.lacinia.schema :as gql-schema]))

(defn conformer
  "Create a conformer instance from a function."
  [fn]
  (gql-schema/as-conformer fn))

(defn compile
  "Compile schema and additionally attach resolvers."
  ([schema] (compile schema {}))
  ([schema resolvers]
   {:pre [(map? schema)
          (map? resolvers)]}
   (-> schema
       (gql-util/attach-resolvers resolvers)
       (gql-schema/compile))))

(defn as-error-map
  "Convert an exception in a graphql-like error map."
  [err]
  {:errors [(gql-util/as-error-map err)]
   :stacktrace (with-out-str (st/print-stack-trace err))})

(defn execute
  "Execute a query on the provided schema."
  ([schema query params] (execute schema query params {}))
  ([schema query params context]
   (try
     (gql/execute schema query params context)
     (catch Throwable e
       (as-error-map e)))))

