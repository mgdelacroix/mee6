(ns hal.logging
  (:require [taoensso.timbre :as log]))

(log/merge-config!
 {:timestamp-opts {:pattern "yyyy-MM-dd HH:mm:ss"
                   :locale :jvm-default
                   :timezone :utc}})

(defmacro inf
  [& args]
  `(log/info ~@args))

(defmacro err
  [& args]
  `(log/error ~@args))

(defmacro dbg
  [& args]
  `(log/debug ~@args))
