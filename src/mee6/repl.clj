(ns mee6.repl
  (:require [clojure.tools.namespace.repl :as repl]
            [mount.core :as mount]
            [mee6.logging :as log]
            [mee6.core :as core]
            [mee6.config :as cfg]))

(defn start
  []
  (mount/start))

(defn stop
  []
  (mount/stop))

(defn restart
  []
  (stop)
  (repl/refresh :after 'mee6.repl/start))
