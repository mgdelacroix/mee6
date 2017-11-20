(ns mee6.ssh
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn run
  [{:keys [uri key] :as host} cmd]
  ;; TODO: add spec for host
  ;;       handle key
  (apply shell/sh "timeout" "5" "ssh"
         uri (str/split cmd #"\s+")))

(defn copy
  [{:keys [uri] :as host} local-path remote-path]
  ;; TODO: add spec for host
  ;;       handle key
  (let [res (shell/sh "scp" local-path (str uri ":" remote-path))
        exit (:exit res)]
    (when-not (= exit 0)
      (throw (ex-info (str "File " local-path " failed to copy to " uri) res)))
  remote-path))
