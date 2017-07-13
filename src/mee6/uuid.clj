(ns mee6.uuid)

(defn random
  []
  (java.util.UUID/randomUUID))

(defn random-str
  []
  (str (random)))

(defn from-string
  "Parse string uuid representation into proper UUID instance."
  [s]
  (java.util.UUID/fromString s))
