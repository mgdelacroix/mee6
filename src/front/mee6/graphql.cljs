(ns mee6.graphql
  (:require [clojure.walk :as walk]
            [rxhttp.browser :as http]
            [beicon.core :as rx]
            [cuerdas.core :as str]
            [mee6.config :as cfg]
            [mee6.store :as st]
            [mee6.util.router :as rt]))

(defn- parse-response
  [{:keys [status body] :as res}]
  (if (http/success? res)
    (-> (js/JSON.parse body)
        (js->clj :keywordize-keys true))
    (throw (ex-info "Error when processing request" res))))

(defn process-keys
  "Recursively transforms all map keys from strings to kebab case."
  [m]
  (let [kebab-keyword (comp keyword str/kebab)
        f (fn [[k v]] [(kebab-keyword k) v])]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(defn- handle-response
  [{:keys [data errors]}]
  (if errors
    (rx/throw (first errors))
    (rx/just (process-keys data))))

(defn query
  ([text]
   (query text nil))
  ([text params]
   (let [body (js/JSON.stringify (clj->js {:query text :variables params}))
         headers {:content-type "application/json"}
         request {:method :post
                  :url cfg/url
                  :body body
                  :headers headers}]
     (->> (http/send! request {:credentials? true})
          (rx/map parse-response)
          (rx/mapcat handle-response)))))
