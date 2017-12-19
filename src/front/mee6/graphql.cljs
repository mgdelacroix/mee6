(ns mee6.graphql
  (:require [rxhttp.core :as http]
            [beicon.core :as rx]))

(defn parse-response
  [{:keys [status body] :as res}]
  (if (http/success? res)
    (-> (js/JSON.parse body)
        (js->clj :keywordize-keys true)
        (:data))
    (throw (ex-info "Error when processing request" res))))

(defn query
  ([text]
   (query text nil))
  ([text params]
   (let [body (js/JSON.stringify (clj->js {:query text :variables params}))
         headers {:content-type "application/json"}
         url "http://localhost:3001/graphql"]
     (->> (http/send! {:method :post :url url :body body :headers headers})
          (rx/map parse-response)))))
