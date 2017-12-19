(ns mee6.engine
  "Monitoring engine namespace."
  (:require [clojure.spec.alpha :as s]
            [clojure.stacktrace :as st]
            [cuerdas.core :as str]
            [mount.core :as mount :refer [defstate]]
            [mee6.config :as cfg]
            [mee6.database :refer [state]]
            [mee6.logging :as log]
            [mee6.modules :as mod]
            [mee6.scheduler :as schd]
            [mee6.services :as sv]
            [mee6.util.crypto :as crypto]
            [mee6.util.time :as dt]))

;; --- Checks Parsing & Reconciliation

(defn- assoc-identifier
  "Given a check object calculates the unique identifier and return
  a check with associated `:id` attribute."
  [check]
  (let [data (dissoc check :name :cron :notify)
        hash (crypto/digest-data data)]
    (assoc check :id hash)))

(defn- resolve-host
  [{:keys [hosts] :as config} id]
  (if (= id "localhost")
    ::localhost ;; a special case
    (let [id (keyword id)]
      (some-> (get hosts id)
              (assoc :id id)))))

(defn- resolve-notify-group
  [{:keys [notify] :as config} id]
  (when (string? id)
    (get notify (keyword id))))

(defn- calculate-checks
  [config]
  (for [check (:checks config)
        hostid (:hosts check)
        :let [host (resolve-host config hostid)]
        :while host]
    (-> (dissoc check :hosts)
        (assoc :host host)
        (assoc-identifier))))

(defn- load-checks
  [config]
  (vec (calculate-checks config)))

(defstate checks
  :start (load-checks cfg/config))

;; --- Engine Impl

(defn- unwrap-exception
  [e]
  (let [message (.getMessage e)]
    (if (instance? clojure.lang.ExceptionInfo e)
      (-> (ex-data e)
          (assoc :message message))
      {:message message
       :stacktrace (with-out-str (st/print-stack-trace e))})))

(defn- notify-change!
  [& {:keys [check config local error current-status previous-status]}]
  (doseq [id (or (:notify check) [])]
    (when-let [group (resolve-notify-group config id)]
      (sv/emit! (keyword "notifications" (:type group))
                {:check check
                 :local local
                 :error error
                 :notify-group group
                 :current-status current-status
                 :previous-status previous-status}))))

(defn- execute-check
  "A function executed by the quartz job to run the check."
  [config {:keys [id] :as check} {:keys [status local] :as data}]
  (let [[curr-status local] (mod/execute check local)]
    (swap! state update-in [:checks id]
           (fn [result]
             (-> (dissoc result :error)
                 (assoc :status curr-status)
                 (assoc :updated-at (dt/now))
                 (assoc :local local))))
    (when (or (and status (not= status curr-status))
              (and (not status) (not= curr-status :green)))
      (notify-change! :config config
                      :check check
                      :local local
                      :current-status curr-status
                      :previous-status status))))

(defn- handle-exception
  [config {:keys [id] :as check} exception]
  (let [prev-status (get-in @state [:checks id :status])
        error (unwrap-exception exception)]
    (swap! state update-in [:checks id]
           (fn [result]
             (-> result
                 (assoc :status :grey)
                 (assoc :updated-at (dt/now))
                 (assoc :error error))))
    (when (or (not prev-status) (not= prev-status :grey))
      (notify-change! :config config
                      :check check
                      :error error
                      :current-status :grey
                      :previous-status prev-status))))

(defn- check-runner
  [{:keys [id name host] :as check} config]
  (let [start (. System (nanoTime))
        data (get-in @state [:checks id])]
    (log/dbg (str/istr "Running check ~{id} \"~{name}\" on ~(:uri host)."))
    (try
      (execute-check config check data)
      (catch Throwable exception
        (handle-exception config check exception))
      (finally
        (let [ms (/ (double (- (. System (nanoTime)) start)) 1000000.0)]
          (log/dbg (str/istr "Check ~{id} finished in ~{ms}ms.")))))))

(defn- engine?
  [v]
  (boolean (::engine v)))

(defn start
  "Start the monitoring engine."
  [scheduler config]
  (letfn [(schedule-job [ctx]
            (let [opts (select-keys ctx [:cron :interval])]
              (schd/schedule! scheduler check-runner [ctx config] opts)))]
    (log/inf "Starting monitoring engine.")
    (let [jobs (reduce #(conj %1 (schedule-job %2)) [] checks)]
      (log/inf "Started" (count jobs) "jobs.")
      {::engine true
       ::jobs jobs
       ::scheduler scheduler})))

(defn stop
  "Stop the monitoring engine."
  [{:keys [::jobs ::scheduler] :as engine}]
  {:pre [(engine? engine)]}
  (log/inf "Stoping monitoring engine.")
  (let [unschedule-job (partial schd/unschedule! scheduler)]
    (run! unschedule-job jobs)))

(defstate engine
  :start (start schd/scheduler cfg/config)
  :stop (stop engine))
