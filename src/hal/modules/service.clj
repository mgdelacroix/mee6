(ns hal.modules.service)

(defn run
  [session ctx]
  {:status :ok  ;; :ko, out of systemctl status
   :last-log ["" "" ""]})  ;; last lines of the journalctl

(defn check
  [{:keys [status]} ctx]
  (if (= status :ok)
    :green
    :red))
