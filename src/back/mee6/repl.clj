(ns mee6.repl
  (:require [clojure.tools.namespace.repl :as repl]
            [mount.core :as mount]
            [mee6.core]))

;; set to avoid classpath collision when reloading
(repl/set-refresh-dirs "src/back")

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
