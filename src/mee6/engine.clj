(ns mee6.engine
  "Monitoring engine namespace."
  (:require [clojure.spec.alpha :as s]
            [mount.core :as mount :refer [defstate]]
            [cuerdas.core :as str]
            [mee6.transit :as t]
            [mee6.database :refer [state]]
            [mee6.scheduler :as schd]
            [mee6.config :as cfg]
            [mee6.uuid :as uuid]
            [mee6.notifications :as notifications]
            [mee6.logging :as log])
  (:import java.time.Instant
           java.security.MessageDigest
           java.util.Base64))

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
        dgst (MessageDigest/getInstance "SHA-256")
        b64e (Base64/getUrlEncoder)]
    (.update dgst data 0 (count data))
    (->> (.digest dgst)
         (.encodeToString b64e))))

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

(defn- wrap-fvar
  [fvar]
  (fn [& [ctx & rest]]
    (try
      (apply fvar ctx rest)
      (catch Throwable e
        e))))

(defn- resolve-module
  "Resolve the module and return the `run` and `check` functions."
  [name]
  (let [ns (str "mee6.modules." name)
        run-symbol (symbol (str ns "/run"))
        check-symbol (symbol (str ns "/check"))]
    (require (symbol ns))
    (let [run-var (resolve run-symbol)
          check-var (resolve check-symbol)]
      [(wrap-fvar run-var)
       (wrap-fvar check-var)])))

(defn- notify-exception
  [{:keys [id] :as ctx} exception]
  (let [{prev-status :status} (get-in @state [:results id])
        data (unwrap-exception exception)]
    (swap! state assoc-in [:results id] {:status :grey
                                         :output data})
    (when (or (not prev-status)
              (not= prev-status :grey))
      (notifications/send-exception-all ctx data))))

(defn- notify-normal
  [{:keys [id] :as ctx} curr-status output]
  (let [{prev-status :status} (get-in @state [:results id])]
    (swap! state assoc-in [:results id] {:status curr-status
                                         :output output})
    (if prev-status
      (when (not= prev-status curr-status)
        (notifications/send-all ctx curr-status output))
      (when (not= curr-status :green)
        (notifications/send-all ctx curr-status output)))))

(defn- check-runner
  "A function executed by the quartz job to run the check."
  [{:keys [id module host notify name] :as ctx}]
  (let [[run check] (resolve-module module)
        output (run ctx)]
    (if (exception? output)
      (notify-exception ctx output)
      (let [status (check ctx output)]
        (if (exception? status)
          (notify-exception ctx status)
          (notify-normal ctx status output))))))

(defn- logged-check-runner
  [{:keys [id name host] :as ctx}]
  (let [start (. System (nanoTime))]
    (log/dbg (str/istr "Running check ~{id} \"~{name}\" on ~(:hostname host)."))
    (try
      (check-runner ctx)
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
              (schd/schedule! scheduler logged-check-runner [ctx] opts)))]
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
