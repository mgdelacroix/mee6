(ns hal.engine
  (:require [hal.scheduler :as schd]))

(defrecord Engine [jobs scheduler])

(defn engine?
  "Check if provided valie is Engine instance."
  [v]
  (instance? Engine v))

(defn- get-checks
  [{:keys [hosts checks notify]}]
  (for [check checks
        hostname (:hosts check)]
    (let [host (get hosts hostname)
          notify (get notify (:notify check))]
      (assoc check
             :host host
             :notify notify))))

(defn- job-impl
  [{:keys [module host] :as ctx}]
  ;; cuando me toca
  ;;  - create ssh session
  ;;  - exec run
  ;;  - error? if so, notify
  ;;  - if not, exec check
  ;;  - notify if proceed
  ;;  - persist result /w timestamp
  (let [{:keys [run check]} (resolve-module module)
        session (ssh/start-session host)
        result (run session ctx)]
    ;; ...
    ))

(defn start
  "Start the monitoring engine."
  [scheduler config]
  (let [checks (get-checks config)
        schedule-job (partial schd/schedule-job! scheduler job-impl)
        jobs (reduce #(conj %1 (schedule-job %2)) [] checks)]
    (->Engine jobs scheduler)))


(defn stop
  "Stop the monitoring engine."
  [{:keys [jobs scheduler] :as engine}]
  {:pre [(engine? engine)]}
  (let [unschedule-job (partial schd/unschedule-job! scheduler)]
    (run! unschedule-job jobs)))
