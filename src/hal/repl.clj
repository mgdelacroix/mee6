(ns hal.repl
  (:require [clojure.tools.namespace.repl :as repl]
            [mount.core :as mount]
            [hal.core :as core]))

(defn start
  []
  (mount/start))

(defn stop
  []
  (mount/stop))

(defn restart
  []
  (stop)
  (repl/refresh :after 'user/start))
