(ns hal.notifications
  (:require [hal.logging :as log])
  (:refer-clojure :exclude [send]))

(defn send
  [ctx status result email]

  (log/inf ">>>> NOTIFICATION")
  (log/inf " - Email ::" email)
  (log/inf " - Status ::" status)
  (log/inf " - Result ::" result)
  (log/inf " - Context ::" ctx))

(defn send-all
  [{:keys [notify] :as ctx} status result]
  (run! #(send ctx status result %) (:emails notify)))

(defn send-exception-all
  [{:keys [notify] :as ctx} result]
  (run! #(send ctx :grey result %) (:emails notify)))
