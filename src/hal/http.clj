(ns hal.http
  (:require [mount.core :refer [defstate]]
            [ring.adapter.jetty :as jetty]
            [hal.config :as cfg]
            [hal.http.home :as home]
            [hal.http.detail :as detail]))

;; --- Router

(defn not-found
  [request]
  {:body "not found"})

(defn router
  [{:keys [uri] :as request}]
  (cond
    (re-matches #"^/$" uri) (home/handler request)
    (re-matches #"^/detail/[^\/]+" uri) (detail/handler request)
    :else (not-found request)))

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
