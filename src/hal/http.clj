(ns hal.http
  (:require [mount.core :refer [defstate]]
            [ring.adapter.jetty :as jetty]
            [hal.config :as cfg]
            [hal.http.home :as home]
            [hal.http.detail :as detail]))

;; --- Router

(declare not-found)

(def routes
  [[#"^/$" #'home/handler]
   [#"^/detail/([^\/]+)" #'detail/handler]])

(defn router
  [{:keys [uri] :as request}]
  (let [response (reduce (fn [acc [re handler]]
                           (when-let [matches (re-matches re uri)]
                             (-> (assoc request :matches matches)
                                 (handler)
                                 (reduced))))
                         nil
                         routes)]
    (if (nil? response)
      (not-found request)
      response)))

(defn not-found
  [request]
  {:body "not found"})

;; --- API

(def defaults
  {:port 3000
   :daemon? false
   :join? false})

(defn start
  [{:keys [http] :as config}]
  (let [options (merge defaults http)]
    (jetty/run-jetty #'router options)))

(defn stop
  [server]
  (.stop server))

(defstate server
  :start (start cfg/config)
  :stop (stop server))
