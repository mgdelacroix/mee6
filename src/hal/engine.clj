(ns hal.engine
  (:require [hal.scheduler :as schd]
            [hal.notifications :as notifications]))

(defrecord Engine [jobs scheduler])

(defn engine?
  "Check if provided valie is Engine instance."
  [v]
  (instance? Engine v))

(defn- get-by-id
  [coll id]
  (->> (keyword id)
       (get coll)))

(defn- get-checks
  [{:keys [hosts checks notify]}]
  (for [check checks
        hostname (:hosts check)]
    (let [host (get-by-id hosts hostname)
          notify (get-by-id notify (:notify check))]
      (when (and host notify)
        (assoc check
               :host host
               :notify notify)))))

(defn- get-safe-checks
  [config]
  (->> (get-checks config)
       (filter #(and (:host %) (:notify %)))))

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
  [{:keys [module host notify name] :as ctx}]
  (println "job-impl:" host notify name)

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
    (let [status (check result ctx)]
      (if (= status :red)
        (notifications/send-all ctx status)))
    ;; persist
    ))

(defn start
  "Start the monitoring engine."
  [scheduler config]
  (let [checks (get-safe-checks config)
        schedule-job (partial schd/schedule! scheduler job-impl)
        jobs (reduce #(conj %1 (schedule-job [%2])) [] checks)]
    (println "jobs" jobs)
    (->Engine jobs scheduler)))

(defn stop
  "Stop the monitoring engine."
  [{:keys [jobs scheduler] :as engine}]
  (let [unschedule-job (partial schd/unschedule! scheduler)]
    (run! unschedule-job jobs)))
