(ns hal.uuid)

(defn random
  []
  (java.util.UUID/randomUUID))

(defn random-str
  []
  (str (random)))
