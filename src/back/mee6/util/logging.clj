(ns mee6.util.logging
  (:require [taoensso.timbre :as log]
            [mount.core :refer [defstate]]))

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

(defn configure!
  [data]
  (log/merge-config! data))
