(ns hal.notifications
  (:refer-clojure :exclude [send]))

(defn send
  [ctx status result email]

  (println ">>>> NOTIFICATION")
  (println " - Email ::" email)
  (println " - Status ::" status)
  (println " - Result ::" result)
  (println " - Context ::" ctx))

(defn send-all
  [{:keys [notify] :as ctx} status result]
  (println "ESTOY NOTIFICANDOOOOOOO A TODOOOOOOOOOOOH" notify)
  (run! #(send ctx status result %) (:emails notify)))

(defn send-exception-all
  [{:keys [notify] :as ctx} result]
  (run! #(send ctx :grey result %) (:emails notify)))
