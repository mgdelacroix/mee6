(ns mee6.graphql
  (:require [rxhttp.core :as http]
            [beicon.core :as rx]
            [mee6.util.router :as rt]
            [mee6.store :as st]))

(defn parse-response
  [{:keys [status body] :as res}]
  (if (http/success? res)
    (-> (js/JSON.parse body)
        (js->clj :keywordize-keys true))
    (throw (ex-info "Error when processing request" res))))

(defn check-login
  [{[error] :errors :as body}]
  (if (= (:type error) "not-authorized")
    (st/emit! (rt/navigate :login))))

(defn query
  ([text]
   (query text nil))
  ([text params]
   (let [body (js/JSON.stringify (clj->js {:query text :variables params}))
         headers {:content-type "application/json"}
         url "http://localhost:3001/graphql"]
     (->> (http/send! {:method :post :url url :body body :headers headers})
          (rx/map parse-response)
          (rx/do check-login)
          (rx/map :data)))))
