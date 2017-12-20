(ns mee6.core
  (:require [mount.core :as mount :refer [defstate]]
            [mee6.scheduler]
            [mee6.http]
            [mee6.services]
            [mee6.services.email]
            [mee6.config :as cfg]
            [mee6.engine :as ngx]
            [mee6.util.logging :as log]
            [mee6.util.yaml :as yaml]
            [mee6.util.crypto :as crypto]
            [mee6.util.docopt :as docopt])
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

(defn- start-server
  [config]
  (when config (alter-var-root #'cfg/*config-path* (constantly config)))
  (try
    (mount/start)
    (catch Exception e
      (let [cause (.getCause e)]
        (handle-error cause)))))

(defn- generate-password
  [username]
  (let [console (System/console)]
    (.printf console "Please enter your password: " (into-array String []))
    (let [passwd (String. (.readPassword console))
          passwd (crypto/derive-password passwd)]
      (println "Please, put this under http -> users section:")
      (println (yaml/encode {username passwd})))))

(def ^:private doc
  "I'm Mr. Meeseeks! Look at me!

Usage: mee6 [options]

Options:
  -c --config=<path>              Set a specific configuration file.
  --generate-password=<username>  Generate a auth entry.
  -h --help                       Show this screen.")

(defn -main
  [& args]
  (let [argm (docopt/parse doc args)]
    (cond
      (or (nil? argm)
          (argm "--config"))
      (start-server (argm "--config"))

      (argm "--help")
      (println doc)

      (argm "--generate-password")
      (generate-password (argm "--generate-password"))

      :else
      (println "Wrong arguments"))))
