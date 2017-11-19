(ns mee6.modules.http-keepalive
  (:require [mee6.modules :as mod]
            [cuerdas.core :as str]
            [rxhttp.core :as http]))

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
  (let [method (keyword (extract-http-method ctx))
        url (:uri host)]
    (http/send!! {:method method :url url})))

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
