(ns hal.core
  (:require [hal.config :as cfg]))

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


(defn start
  []
  (let [config (cfg/parse "resources/config.yml")]
    ;; do nothing
    ))

(defn -main
  [& [path]]
  (try
    (start)
    (catch Exception e
      (handle-error e))))
