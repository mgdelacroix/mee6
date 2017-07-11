(ns hal.notifications
  (:refer-clojure :exclude [send]))

(defn send
  [ctx status email]

  (println ">>>> NOTIFICATION")
  (println " - Email ::" email)
  (println " - Status ::" status)
  (println " - Context ::" ctx))

(defn send-all
  [{:keys [notify] :as ctx} status]
  (run! send (:emails notify)))
