(ns mee6.engine
  "Monitoring engine namespace."
  (:require [clojure.spec.alpha :as s]
            [mount.core :as mount :refer [defstate]]
            [cuerdas.core :as str]
            [mee6.state :refer [state]]
            [mee6.scheduler :as schd]
            [mee6.config :as cfg]
            [mee6.uuid :as uuid]
            [mee6.notifications :as notifications]
            [mee6.logging :as log])
  (:import java.time.Instant))

;; --- Spec

(s/def ::jobs (s/coll-of string? :kind vector?))
(s/def ::scheduler any?)
(s/def ::engine (s/keys :req-un [::jobs ::scheduler]))

;; --- Impl

(defn- engine?
  [v]
  (s/valid? ::engine v))

(defn- get-by-id
  "Helper that retrieves the value referenced by the
  keywordiced id."
  [coll id]
  (->> (keyword id)
       (get coll)))

(defn- get-checks
  "Return a list of checks obtained from the provided configuration
  object."
  [{:keys [hosts checks notify]}]
  (for [check checks
        hostname (:hosts check)]
    (let [host (get-by-id hosts hostname)
          notify (get-by-id notify (:notify check))]
      (when (and host notify)
        (assoc check
               :id (uuid/random)
               :host host
               :notify notify)))))

(defn- get-safe-checks
  "Safely obtain a list of checks from the configuration, filtering
  the incomplete ones."
  [config]
  (->> (get-checks config)
       (remove nil?)))

(defn- ex-info?
  [v]
  (instance? clojure.lang.ExceptionInfo v))

(defn- exception?
  [v]
  (instance? Throwable v))

(defn- unwrap-exception
  [e]
  (let [data {:message (.getMessage e)}]
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
  (let [data (unwrap-exception exception)]
    (swap! state assoc-in [:results id] {:status :grey
                                         :output data
                                         :updated-at (Instant/now)})
    (notifications/send-exception-all ctx data)))

(defn- notify-normal
  [{:keys [id] :as ctx} curr-status output]
  (let [{prev-status :status} (get-in @state [:results id])]
    (swap! state assoc-in [:results id] {:status curr-status
                                         :output output
                                         :updated-at (Instant/now)})
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

(defn start
  "Start the monitoring engine."
  [scheduler config]
  {:pre [(any? scheduler)
         (cfg/config? config)]}
  (letfn [(schedule-job [ctx]
            (let [opts (select-keys ctx [:cron :interval])]
              (schd/schedule! scheduler logged-check-runner [ctx] opts)))]
    (log/inf "Starting monitoring engine.")
    (let [checks (get-safe-checks config)
          jobs (reduce #(conj %1 (schedule-job %2)) [] checks)]
      (swap! state assoc :checks checks)
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
