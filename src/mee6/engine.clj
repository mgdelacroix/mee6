(ns mee6.engine
  "Monitoring engine namespace."
  (:require [clojure.spec.alpha :as s]
            [mount.core :as mount :refer [defstate]]
            [cuerdas.core :as str]
            [mee6.time :as dt]
            [mee6.transit :as t]
            [mee6.modules :as mod]
            [mee6.database :refer [state]]
            [mee6.scheduler :as schd]
            [mee6.config :as cfg]
            [mee6.uuid :as uuid]
            [mee6.notifications :as notifications]
            [mee6.logging :as log])
  (:import java.security.MessageDigest
           org.apache.commons.codec.binary.Base64))

;; --- Spec

(s/def ::jobs (s/coll-of string? :kind vector?))
(s/def ::scheduler any?)
(s/def ::engine (s/keys :req-un [::jobs ::scheduler]))

;; --- Checks Parsing & Reconciliation

(defn- get-by-keywordized-id
  "Helper that retrieves the value referenced by the
  keywordiced id."
  [coll id]
  {:pre [(string? id)]}
  (->> (keyword id)
       (get coll)))

(defn- digest
  "Given the check essential data, calculates the sha256 hash of
  its msgpack representation."
  [data]
  (let [data (t/encode data {:type :msgpack})
        dgst (MessageDigest/getInstance "SHA-256")]
    (.update dgst data 0 (count data))
    (Base64/encodeBase64URLSafeString (.digest dgst))))

(defn- assoc-identifier
  "Given a check object calculates the unique identifier and return
  a check with associated `:id` attribute."
  [check]
  (let [data (dissoc check :name :cron :notify)
        hash (digest data)]
    (assoc check :id hash)))

(defn- get-checks
  [{:keys [hosts checks notify] :as config}]
  (for [check checks
        hostname (:hosts check)
        :let [host (get-by-keywordized-id hosts hostname)
              notify (get-by-keywordized-id notify (:notify check))]
        :while (and host notify)]
    (-> (dissoc check :hosts)
        (assoc :host host :notify notify)
        (assoc-identifier))))

(defn- ex-info?
  [v]
  (instance? clojure.lang.ExceptionInfo v))

(defn- exception?
  [v]
  (instance? Throwable v))

(defn- unwrap-exception
  [e]
  (let [data {:message (.getMessage e)
              :out (with-out-str (clojure.stacktrace/print-stack-trace e))}]
    (cond-> data
      (ex-info? e) (merge (ex-data e)))))

(defn- safe-resolve
  [sym]
  (try
    (resolve sym)
    (catch Throwable e
      nil)))

(defn- resolve-module
  "Resolve the module and return the `run` and `check` functions."
  [name ctx]
  (let [mns (str "mee6.modules." name)
        sym (symbol (str mns "/instance"))]
    (require (symbol mns))
    (let [factory (safe-resolve sym)]
      (when (nil? factory) (throw (ex-info "Module does not exists" {:name name})))
      (factory ctx))))

(defn- execute-check
  "A function executed by the quartz job to run the check."
  [{:keys [id module host notify name] :as ctx}]
  (let [module (resolve-module module ctx)
        data (get-in @state [:check id] {})
        local (mod/-run module (:local data))
        prev-status (:status data)
        curr-status (mod/-check module local)]
    (swap! state update-in [:checks id]
           (fn [result]
             (-> result
                 (dissoc :error)
                 (assoc :status curr-status)
                 (assoc :updated-at (dt/now))
                 (assoc :local local))))
    (if prev-status
      (when (not= prev-status curr-status)
        (notifications/send-all ctx curr-status local)
      (when (not= curr-status :green)
        (notifications/send-all ctx curr-status local))))))

(defn- handle-exception
  [{:keys [id] :as ctx} exception]
  (let [prev-status (get-in @state [:checks id :status])
        error (unwrap-exception exception)]
    (swap! state update-in [:checks id]
           (fn [result]
             (-> result
                 (assoc :status :grey)
                 (assoc :updated-at (dt/now))
                 (assoc :error error))))
    (when (or (not prev-status)
              (not= prev-status :grey))
      (notifications/send-exception-all ctx error))))

(defn- check-runner
  [{:keys [id name host] :as ctx}]
  (let [start (. System (nanoTime))]
    (log/dbg (str/istr "Running check ~{id} \"~{name}\" on ~(:hostname host)."))
    (try
      (execute-check ctx)
      (catch Throwable e
        (handle-exception e))
      (finally
        (let [ms (/ (double (- (. System (nanoTime)) start)) 1000000.0)]
          (log/dbg (str/istr "Check ~{id} finished in ~{ms}ms.")))))))

;; --- API

(defn- engine?
  [v]
  (s/valid? ::engine v))

(defn start
  "Start the monitoring engine."
  [scheduler config]
  {:pre [(any? scheduler)
         (cfg/config? config)]}
  (letfn [(schedule-job [ctx]
            (let [opts (select-keys ctx [:cron :interval])]
              (schd/schedule! scheduler check-runner [ctx] opts)))]
    (log/inf "Starting monitoring engine.")
    (let [checks (get-checks config)
          jobs (reduce #(conj %1 (schedule-job %2)) [] checks)]
      (log/inf "Started" (count jobs) "jobs.")
      {:jobs jobs
       :scheduler scheduler})))

(defn stop
  "Stop the monitoring engine."
  [{:keys [jobs scheduler] :as engine}]
  {:pre [(engine? engine)]}
  (log/inf "Stoping monitoring engine.")
  (let [unschedule-job (partial schd/unschedule! scheduler)]
    (run! unschedule-job jobs)))

;; --- State

(defstate engine
  :start (start schd/scheduler cfg/config)
  :stop (stop engine))
