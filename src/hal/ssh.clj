(ns hal.ssh
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn run
  [{:keys [hostname key] :as host} cmd]
  (apply shell/sh "timeout" "5" "ssh"
         hostname (str/split cmd #"\s+")))
