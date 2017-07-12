(ns hal.core
  (:require [mount.core :as mount :refer [defstate]]
            [hal.config :as cfg]
            [hal.scheduler :as schd]
            [hal.engine :as ngx]
            [hal.logging :as log]))

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
