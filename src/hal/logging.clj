(ns hal.logging)

(defn inf
  [& messages]
  (apply println messages))

(defn err
  [& messages]
  (apply println messages))
