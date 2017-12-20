(ns mee6.graphql
  (:require [rxhttp.browser :as http]
            [beicon.core :as rx]
            [mee6.config :as cfg]
            [mee6.store :as st]
            [mee6.util.router :as rt]))

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
         headers {:content-type "application/json"}]
         (->> (http/send! {:method :post :url cfg/url :body body :headers headers} {:credentials? true})
              (rx/map parse-response)
              (rx/do check-login)
              (rx/map :data)))))
