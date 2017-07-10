(ns hal.ssh
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn run
  [host cmd]
  (apply shell/sh "ssh" host (str/split cmd #"\s+")))
