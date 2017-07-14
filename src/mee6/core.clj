(ns mee6.core
  (:require [mount.core :as mount :refer [defstate]]
            [mee6.config :as cfg]
            [mee6.engine :as ngx]
            [mee6.scheduler]
            [mee6.http]
            [mee6.logging :as log])
  (:gen-class))

(defn- handle-error
  [err]
  (if (instance? clojure.lang.ExceptionInfo err)
    (let [message (.getMessage err)
          payload (ex-data err)]
      (log/err message)
      (log/err payload)
      (System/exit -1))

    (let [message (.getMessage err)]
      (log/err message)
      (.printStackTrace err)
      (System/exit -2))))

(defn -main
  [& [path]]
  (try
    (mount/start)
    (catch Exception e
      (let [cause (.getCause e)]
        (handle-error cause)))))