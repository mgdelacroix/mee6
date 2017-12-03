(ns mee6.modules.http-keepalive
  (:require [cuerdas.core :as str]
            [cheshire.core :as json]
            [mee6.cmd :as cmd]
            [mee6.modules :as mod]))

(defn- extract-http-method
  [{:keys [http-method] :as ctx}]
  (let [allowed-methods #{"head", "options", "get"}]
    (if (string? http-method)
      (get allowed-methods (str/lower http-method) "head")
      "head")))

(defn- extract-expected-statuses
  [{:keys [expected-statuses] :as ctx}]
  (if (and (seq expected-statuses)
           (every? int? expected-statuses))
    (set expected-statuses)
    #{200, 201, 204}))

(defn- perform-request
  [{:keys [host url] :as ctx}]
  (let [method (extract-http-method ctx)
        result (cmd/run-script host "http_keepalive" {:url url :method method})]
    (if (zero? (:exit result))
      (json/decode (:out result) true)
      (throw (ex-info "Unexpected exit code when executing the script." result)))))

(defn- run
  [ctx local]
  (let [{:keys [status latency]} (perform-request ctx)]
    (assoc local :status status :latency latency)))

(defn- check
  [ctx local]
  (let [expected-statuses (extract-expected-statuses ctx)
        status (:status local)]
    (if (expected-statuses status) :green :red)))

(defn instance
  [ctx]
  (reify
    mod/IModule
    (-run [_ local] (run ctx local))
    (-check [_ local] (check ctx local))))
