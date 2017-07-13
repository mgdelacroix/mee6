(ns hal.http
  (:require [mount.core :refer [defstate]]
            [ring.adapter.jetty :as jetty]
            [hal.config :as cfg]))

;; --- Router

(defn router
  [request]
  {:body "hello world"})

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
