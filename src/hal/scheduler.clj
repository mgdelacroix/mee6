(ns hal.scheduler
  (:require [twarc.core :as twarc]
            [com.stuartsierra.component :as component]))

(def scheduler-options
  {:threadPool.threadCount 1
   :threadPool.threadPriority Thread/MIN_PRIORITY
   :threadPool.makeThreadsDaemons true})

(defn- run-dynamic-job
  [scheduler implfn & args]
  (apply implfn args))

(defn start
  "Start new quartz scheduler."
  []
  (-> (twarc/make-scheduler scheduler-options)
      (twarc/start)))

(defn stop
  "Stop the scheduler."
  [sched]
  (component/stop sched))

(defn schedule-job!
  [scheduler implfn {:keys [cron] :as ctx}]
  {:pre [(fn? implfn)
         (string? cron)]}
  (let [id (str (uuid/random))]
    (twarc/schedule-job scheduler #'run-dynamic-job [implfn ctx]
                        :job {:identity id}
                        :trigger {:cron cron})
    id))

(defn unschedule-job!
  [scheduler jobid]
  {:pre [(string? jobid)]}
  (twarc/delete-job scheduler jobid))

