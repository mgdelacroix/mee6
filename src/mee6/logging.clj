(ns mee6.logging
  (:require [taoensso.timbre :as log]))

(log/merge-config!
 {:timestamp-opts {:pattern "yyyy-MM-dd HH:mm:ss"
                   :locale :jvm-default
                   :timezone :utc}})

(defn disable
  []
  (log/merge-config!
   {:ns-blacklist ["mee6.*"]}))

(defn enable
  []
  (log/merge-config!
   {:ns-blacklist []}))

(defmacro inf
  [& args]
  `(log/info ~@args))

(defmacro err
  [& args]
  `(log/error ~@args))

(defmacro dbg
  [& args]
  `(log/debug ~@args))
