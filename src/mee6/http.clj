(ns mee6.http
  (:require [mount.core :refer [defstate]]
            [ring.adapter.jetty :as jetty]
            [mee6.config :as cfg]
            [mee6.http.home :as home]
            [mee6.http.detail :as detail]))

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
  (when http
    (let [options (merge defaults http)]
      (jetty/run-jetty #'router options))))

(defn stop
  [server]
  (when server
    (.stop server)))

(defstate server
  :start (start cfg/config)
  :stop (stop server))