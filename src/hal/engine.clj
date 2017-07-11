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

(defn- resolve-module
  [name]
  (let [ns (str "hal.modules." name)
        run-symbol (symbol (str ns "/run"))
        check-symbol (symbol (str ns "/check"))]
    (require (symbol ns))
    (let [run-var (resolve run-symbol)
          check-var (resolve check-symbol)]
      [run-var check-var])))

(defn- job-impl
  [{:keys [module host] :as ctx}]
  ;; cuando me toca
  ;;  - create ssh session
  ;;  - exec run
  ;;  - error? if so, notify
  ;;  - if not, exec check
  ;;  - notify if proceed
  ;;  - persist result /w timestamp
  (let [[run check] (resolve-module module)
        result (run nil ctx)]


    ;; TEST FOR ERROR
    (case (check result ctx)
      :green
      :red
      ;; notify
      ;; else notify bad return value
      )
    ;; persist
    ))

(defn start
  "Start the monitoring engine."
  [scheduler config]
  (let [checks (get-checks config)
        schedule-job (partial schd/schedule! scheduler job-impl)
        jobs (reduce #(conj %1 (schedule-job %2)) [] checks)]
    (->Engine jobs scheduler)))


(defn stop
  "Stop the monitoring engine."
  [{:keys [jobs scheduler] :as engine}]
  {:pre [(engine? engine)]}
  (let [unschedule-job (partial schd/unschedule! scheduler)]
    (run! unschedule-job jobs)))
