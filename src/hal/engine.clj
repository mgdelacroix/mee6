(ns hal.engine
  (:require [hal.scheduler :as schd]
            [hal.uuid :as uuid]
            [hal.notifications :as notifications]
            [hal.logging :as log]))

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
               :id (uuid/random)
               :host host
               :notify notify)))))

(defn- get-safe-checks
  [config]
  (->> (get-checks config)
       (filter #(and (:host %) (:notify %)))))

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
        (.printStackTrace e)
        e))))

(defn- resolve-module
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

(defn- job-impl
  [{:keys [module host notify name] :as ctx}]
  (let [[run check] (resolve-module module)
        result (run ctx)]
    (if (exception? result)
      (notify-exception ctx result)
      (let [status (check ctx result)]
        (if (exception? status)
          (notify-exception ctx status)
          (when (= status :red)
            (notifications/send-all ctx status result)))))))

(defn start
  "Start the monitoring engine."
  [scheduler config]
  (letfn [(schedule-job [ctx]
            (let [opts (select-keys ctx [:cron :interval])]
              (schd/schedule! scheduler job-impl [ctx] opts)))]
    (let [checks (get-safe-checks config)
          jobs (reduce #(conj %1 (schedule-job %2)) [] checks)]
      (log/inf "jobs" jobs)
      (->Engine jobs scheduler))))

(defn stop
  "Stop the monitoring engine."
  [{:keys [jobs scheduler] :as engine}]
  (let [unschedule-job (partial schd/unschedule! scheduler)]
    (run! unschedule-job jobs)))
