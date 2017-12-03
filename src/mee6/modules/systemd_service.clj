(ns mee6.modules.systemd-service
  (:require [cheshire.core :as json]
            [mee6.cmd :as cmd]
            [mee6.modules :as mod]))

(defn- run
  [{:keys [host service] :as ctx} local]
  (let [result (cmd/run-script host  "systemd_service" {:name service})]
    (if (zero? (:exit result))
      (json/decode (:out result) true)
      (throw (ex-info "Unexpected exit code when executing the script." result)))))

(defn- check
  [ctx {:keys [status] :as local}]
  (if (= status "up")
    :green
    :red))

(defn instance
  [ctx]
  (reify
    mod/IModule
    (-run [_ local] (run ctx local))
    (-check [_ local] (check ctx local))))
