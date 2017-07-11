(ns hal.core
  (:require [mount.core :as mount :refer [defstate]]
            [hal.config :as cfg]
            [hal.engine :as ngx]
            [hal.scheduler :as schd]))

(defstate config
  :start (cfg/load))

(defstate scheduler
  :start (schd/start)
  :stop (schd/stop scheduler))

(defstate engine
  :start (ngx/start scheduler config)
  :stop (ngx/stop engine))

(defn- handle-error
  [err]
  (if (instance? clojure.lang.ExceptionInfo err)
    (let [message (.getMessage err)
          payload (ex-data err)]
      (println message)
      (println payload)
      (System/exit -1))

    (let [message (.getMessage err)]
      (println message)
      (.printStackTrace err)
      (System/exit -2))))

(defn -main
  [& [path]]
  (try
    (mount/start)
    (catch Exception e
      (let [cause (.getCause e)]
        (handle-error cause)))))

