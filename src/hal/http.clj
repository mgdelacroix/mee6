(ns hal.http
  (:require [mount.core :refer [defstate]]
            [ring.adapter.jetty :as jetty]))

(defn router
  [request]
  {:body "hello world"})

(def options
  {:port 3000
   :daemon? true
   :join? false})

(defn start
  []
  (jetty/run-jetty #'router options))

(defstate server
  :start (start)
  :stop (.stop server))
