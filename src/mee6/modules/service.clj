(ns mee6.modules.service
  (:require [mee6.ssh :as ssh]))

(defn get-last-log
  [host service]
  (:out (ssh/run host (str "sudo journalctl -r -n 10 -u " service))))

(defn run
  [{:keys [host service] :as ctx}]
  (let [{:keys [exit] :as result} (ssh/run host (str "systemctl status " service))]
    {:status (if (= exit 0) :up :down)
     :out (get-last-log host service)}))

(defn check
  [ctx {:keys [status] :as result}]
  (if (= status :up)
    :green
    :red))
