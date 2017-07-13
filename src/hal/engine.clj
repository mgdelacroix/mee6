(ns hal.engine
  "Monitoring engine namespace."
  (:require [clojure.spec.alpha :as s]
            [mount.core :as mount :refer [defstate]]
            [cuerdas.core :as str]
            [hal.state :refer [state]]
            [hal.scheduler :as schd]
            [hal.config :as cfg]
            [hal.uuid :as uuid]
            [hal.notifications :as notifications]
            [hal.logging :as log])
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
  (instance? Exception v))

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
      (catch Exception e
        e))))

(defn- resolve-module
  "Resolve the module and return the `run` and `check` functions."
  [name]
  (let [ns (str "hal.modules." name)
        run-symbol (symbol (str ns "/run"))
        check-symbol (symbol (str ns "/check"))]
    (require (symbol ns))
    (let [run-var (resolve run-symbol)
          check-var (resolve check-symbol)]
      [(wrap-fvar run-var)
       (wrap-fvar check-var)])))

(defn- notify-exception
  [ctx data]
  (->> (unwrap-exception data)
       (notifications/send-exception-all ctx)))

(defn- notify-normal
  [{:keys [id] :as ctx} curr-status result]
  (let [{prev-status :status} (get-in @state [:current id])]
    (swap! state assoc-in [:current id] {:status curr-status
                                         :result result
                                         :updated-at (Instant/now)})
    (if prev-status
      (when (not= prev-status curr-status)
        (notifications/send-all ctx curr-status result))
      (when (not= curr-status :green)
        (notifications/send-all ctx curr-status result)))))

(defn- check-runner
  "A function executed by the quartz job to run the check."
  [{:keys [id module host notify name] :as ctx}]
  (let [[run check] (resolve-module module)
        result (run ctx)]
    (if (exception? result)
      (notify-exception ctx result)
      (let [status (check ctx result)]
        (if (exception? status)
          (notify-exception ctx status)
          (notify-normal ctx status result))))))

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
