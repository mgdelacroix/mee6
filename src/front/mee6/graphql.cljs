(ns mee6.graphql
  (:require [rxhttp.browser :as http]
            [beicon.core :as rx]
            [mee6.config :as cfg]
            [mee6.store :as st]
            [mee6.util.router :as rt]))

(defn- parse-response
  [{:keys [status body] :as res}]
  (if (http/success? res)
    (-> (js/JSON.parse body)
        (js->clj :keywordize-keys true))
    (throw (ex-info "Error when processing request" res))))

(defn- handle-response
  [{:keys [data errors]}]
  (if errors
    (rx/throw (first errors))
    (rx/just data)))

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
