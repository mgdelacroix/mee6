(ns mee6.ssh
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn run
  [{:keys [hostname key] :as host} cmd]
  ;; TODO: add spec for host
  ;;       handle key
  (apply shell/sh "timeout" "5" "ssh"
         hostname (str/split cmd #"\s+")))

(defn copy
  [{:keys [hostname] :as host} local-path remote-path]
  ;; TODO: add spec for host
  ;;       handle key
  (let [res (shell/sh "scp" local-path (str hostname ":" remote-path))
        exit (:exit res)]
    (when-not (= exit 0)
      (throw (ex-info (str "File " local-path " failed to copy to " hostname) res)))
  remote-path))
