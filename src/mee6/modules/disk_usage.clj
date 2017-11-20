(ns mee6.modules.disk-usage
  "Disk usage monitoring module."
  (:require [cheshire.core :as json]
            [mee6.cmd :as cmd]
            [mee6.modules :as mod]))

(defn- retrieve-info
  [{:keys [host device] :as ctx}]
  (let [result (cmd/run-script host "disk_usage" {:device device})]
    (if (zero? (:exit result))
      (json/decode (:out result) true)
      (throw (ex-info "Unexpected exit code when executing the script." result)))))

(defn- run
  [ctx local]
  (merge local (retrieve-info ctx)))

(defn- check
  [{:keys [threshold] :as ctx} {:keys [used capacity] :as state}]
  (let [percentage (quot (* used 100) capacity)]
    (if (> percentage threshold)
      :red
      :green)))

(defn instance
  [ctx]
  (reify
    mod/IModule
    (-run [_ local] (run ctx local))
    (-check [_ local] (check ctx local))))
