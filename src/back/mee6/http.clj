(ns mee6.http
  (:require [mount.core :refer [defstate]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [mee6.logging :as log]
            [mee6.config :as cfg]
            [mee6.http.graphql :as graphql]))

;; --- Router

(declare not-found)

(def routes
  [[#"^/graphql$" #'graphql/handle-graphql]
   [#"^/graphiql$" #'graphql/handle-graphiql]])

(defn router
  [{:keys [uri] :as request}]
  (letfn [(handle [_ [re handler]]
            (when-let [matches (re-matches re uri)]
              (-> (assoc request :matches matches)
                  (handler)
                  (reduced))))]
    (as-> (reduce handle nil routes) $
      (or $ (not-found request)))))

(defn not-found
  [request]
  {:status 404
   :body "not found"})

;; --- API

(def defaults
  {:port 3000
   :daemon? false
   :join? false})

(defn start
  [{:keys [http] :as config}]
  (when http
    (log/inf "Starting http server on port" (:port http))
    (let [options (merge defaults http)
          handler (-> router
                      (wrap-keyword-params)
                      (wrap-params)
                      (wrap-cookies)
                      (wrap-json-body {:keywords? true})
                      (wrap-json-response))]
      (jetty/run-jetty handler options))))

(defn stop
  [server]
  (when server
    (.stop server)))

(defstate server
  :start (start cfg/config)
  :stop (stop server))
