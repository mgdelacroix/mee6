(ns mee6.exceptions)

(defn error
  [& {:keys [type message] :or {type :unexpected} :as payload}]
  {:pre [(keyword? type) (string? message)]}
  (let [payload (-> payload
                    (dissoc :message)
                    (assoc :type type))]
    (ex-info message payload)))

(defmacro raise
  [& args]
  `(throw (error ~@args)))
