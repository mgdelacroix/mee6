(ns mee6.logging
  (:require [taoensso.timbre :as log]
            [mount.core :refer [defstate]]
            [mee6.config :as cfg]))

;; --- REPL Helpers

(defn disable
  []
  (log/merge-config!
   {:ns-blacklist ["mee6.*"]}))

(defn enable
  []
  (log/merge-config!
   {:ns-blacklist []}))

;; --- Public API

(defmacro inf
  [& args]
  `(log/info ~@args))

(defmacro err
  [& args]
  `(log/error ~@args))

(defmacro dbg
  [& args]
  `(log/debug ~@args))

;; --- Internal state

(defn- initialize-logging
  [{:keys [log-level] :as cfg}]
  (let [opts {:level (keyword log-level)
              :timestamp-opts {:pattern "yyyy-MM-dd HH:mm:ss"
                               :locale :jvm-default
                               :timezone :utc}}]
    (log/merge-config! opts)))

(defstate logging
  :start (initialize-logging cfg/config))
