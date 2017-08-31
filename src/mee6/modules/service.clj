(ns mee6.modules.service
  (:require [mee6.ssh :as ssh]
            [mee6.modules :as mod]))

(defn get-last-log
  [host service]
  (:out (ssh/run host (str "journalctl -r -n 10 -u " service))))

(defn instance
  [{:keys [host service] :as ctx}]
  (reify
    mod/IModule
    (-run [_ local]
      (let [{:keys [exit] :as result} (ssh/run host (str "systemctl status " service))]
        (assoc local
               :status (if (= exit 0) :up :down)
               :lastlog (get-last-log host service))))

    (-check [_ local]
      (let [status (:status local)]
        (if (= status :up)
          :green
          :red)))))
