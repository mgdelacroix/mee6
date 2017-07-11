(ns hal.ssh
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn run
  [{:keys [hostname key] :as host} cmd]
  ;; TODO: add spec for host
  ;;       handle key
  (println "ssh/run" host cmd)
  (apply shell/sh "ssh" hostname (str/split cmd #"\s+")))

