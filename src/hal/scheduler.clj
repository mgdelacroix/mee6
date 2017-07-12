(ns hal.scheduler
  (:require [mount.core :as mount :refer [defstate]]
            [hal.uuid :as uuid])
  (:import java.util.Properties
           org.quartz.Scheduler
           org.quartz.SchedulerException
           org.quartz.impl.StdSchedulerFactory
           org.quartz.Job
           org.quartz.JobBuilder
           org.quartz.JobDataMap
           org.quartz.JobExecutionContext
           org.quartz.JobKey
           org.quartz.TriggerBuilder
           org.quartz.CronScheduleBuilder
           org.quartz.SimpleScheduleBuilder
           org.quartz.PersistJobDataAfterExecution
           org.quartz.DisallowConcurrentExecution))

;; --- Implementation

(defn- map->props
  [data]
  (let [p (Properties.)]
    (run! (fn [[k v]] (.setProperty p (name k) (str v))) (seq data))
    p))

(deftype JobImpl []
  Job
  (execute [_ context]
    (let [^JobDataMap data (.. context getJobDetail getJobDataMap)
          args (.get data "arguments")
          state (.get data "state")
          callable (.get data "callable")]
      (if state
        (apply callable state args)
        (apply callable args)))))

(defn- build-trigger
  [opts]
  (let [repeat? (:repeat opts true)
        interval (:interval opts 1000)
        cron (:cron opts)
        group (:group opts "hal")
        schdl (if cron
                (CronScheduleBuilder/cronSchedule cron)
                (let [schdl (SimpleScheduleBuilder/simpleSchedule)
                      schdl (if (number? repeat?)
                              (.withRepeatCount schdl repeat?)
                              (.repeatForever schdl))]
                  (.withIntervalInMilliseconds schdl interval)))
        id (str (:id opts) "-trigger")
        bldr (doto (TriggerBuilder/newTrigger)
               (.startNow)
               (.withIdentity id group)
               (.withSchedule schdl))]
    (.build bldr)))

(defn- build-job-detail
  [f args opts]
  (let [state (:state opts)
        group (:group opts "hal")
        id    (str (:id opts))
        data  {"callable" f
               "arguments" (into [] args)
               "state" (if state (atom state) nil)}
        bldr (doto (JobBuilder/newJob JobImpl)
               (.storeDurably false)
               (.usingJobData (JobDataMap. data))
               (.withIdentity id group))]
    (.build bldr)))

(defn- make-scheduler-props
  [{:keys [name daemon? threads thread-priority]
    :or {name "hal-scheduler"
         daemon? true
         threads 1
         thread-priority Thread/MIN_PRIORITY}}]
  (map->props
   {"org.quartz.threadPool.threadCount" threads
    "org.quartz.threadPool.threadPriority" thread-priority
    "org.quartz.threadPool.makeThreadsDaemons" (if daemon? "true" "false")
    "org.quartz.scheduler.instanceName" name
    "org.quartz.scheduler.makeSchedulerThreadDaemon" (if daemon? "true" "false")}))

;; --- Public Api

(defn start
  ([]
   (start nil))
  ([opts]
   (let [props (make-scheduler-props opts)
         factory (StdSchedulerFactory. props)]
     (doto (.getScheduler factory)
       (.start)))))

(defn stop
  [scheduler]
  (.shutdown ^Scheduler scheduler true))

(defn schedule!
  ([scheduler f args] (schedule! scheduler f args nil))
  ([scheduler f args opts]
   (let [id (uuid/random-str)
         opts (merge {:id id} opts)
         job (build-job-detail f args opts)
         trigger (build-trigger opts)]
     (.scheduleJob ^Scheduler scheduler job trigger)
     id)))

(defn unschedule!
  [scheduler jobid]
  {:pre [(string? jobid)]}
  (let [key (JobKey. jobid "hal")]
    (.deleteJob scheduler key)
    nil))

(defstate scheduler
  :start (start)
  :stop (stop scheduler))
