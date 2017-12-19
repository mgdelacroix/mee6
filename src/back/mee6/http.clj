(ns mee6.http
  (:require [cuerdas.core :as str]
            [cheshire.core :as json]
            [mount.core :refer [defstate]]
            [mee6.util.template :as tmpl]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [mee6.logging :as log]
            [mee6.config :as cfg]
            [mee6.database :as db]
            [mee6.graphql :as gql]))

;; --- Router

(def not-found
  (constantly {:status 404 :body "not found"}))

(defn handle-graphiql
  [req]
  (let [encode #(-> (json/encode %) (str/replace #"\/" "\\/"))
        params {:query (encode (get-in req [:params :query] ""))
                :variables (encode (get-in req [:params :variables] ""))
                :operation (encode (get-in req [:params :operationName] ""))}
        body (tmpl/render "graphiql.html" params)]
    (if (get-in cfg/config [:http :graphiql])
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body body}
      (not-found req))))

(defn handle-graphql
  [req]
  (let [rsp (atom {})
        context {:req req :rsp rsp :authenticated (:authenticated req)}
        query (get-in req [:body :query])
        params (get-in req [:body :variables])
        result (gql/execute query params context)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/encode result)}))

(def routes
  [[#"^/graphql$" #'handle-graphql]
   [#"^/graphiql$" #'handle-graphiql]])

(defn router
  [{:keys [uri] :as request}]
  (letfn [(handle [_ [re handler]]
            (when-let [matches (re-matches re uri)]
              (-> (assoc request :matches matches)
                  (handler)
                  (reduced))))]
    (as-> (reduce handle nil routes) $
      (or $ (not-found request)))))

(defn wrap-auth
  [handler]
  (letfn [(valid-token? [token]
            (not (nil? (get-in @db/state [:tokens token]))))]
    (fn [{:keys [cookies] :as request}]
      (let [token (get-in cookies ["auth-token" :value])]
        (if (and token (not (nil? (get-in @db/state [:tokens token]))))
          (handler (assoc request :authenticated true))
          (handler (assoc request :authenticated false)))))))

(defn wrap-errors
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (.printStackTrace e)
        (throw e)))))

;; --- API

(def ^:private defaults
  {:port 3000
   :daemon? false
   :join? false})

(defn- start
  [{:keys [http] :as config}]
  (when http
    (log/inf "Starting http server on port" (:port http))
    (let [options (merge defaults http)]
      (-> router
          (wrap-errors)
          (wrap-cors :access-control-allow-origin [#".*"]
                     :access-control-allow-credentials "true"
                     :access-control-allow-methods [:get :post :options]
                     :access-control-allow-headers [:x-requested-with :content-type :authorization])
          (wrap-auth)
          (wrap-keyword-params)
          (wrap-params)
          (wrap-cookies)
          (wrap-json-body {:keywords? true})
          (wrap-json-response)
          (jetty/run-jetty options)))))

(defn- stop
  [server]
  (when server
    (.stop server)))

(defstate server
  :start (start cfg/config)
  :stop (stop server))
