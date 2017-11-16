(ns mee6.modules.http-keepalive
  (:require [mee6.modules :as mod]
            [cuerdas.core :as str]
            [clj-http.client :as http]))

;; TODO: allow specicy an alert on the latency value

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
  [{:keys [host] :as ctx}]
  (let [method (extract-http-method ctx)
        uri (:uri host)]
    (try
      (case method
        "head" (http/head uri)
        "get" (http/get uri)
        "options" (http/options uri))
      (catch clojure.lang.ExceptionInfo e
        (ex-data e)))))

(defn- run
  [ctx local]
  (let [start-time (System/nanoTime)
        {:keys [status]} (perform-request ctx)
        end-time (System/nanoTime)]
    (assoc local
           :status status
           :latency (/ (double (- end-time start-time)) 1000000.0))))

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
