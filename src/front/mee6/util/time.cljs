(ns mee6.util.time
  (:require [cljsjs.date-fns]))

(def ^:private dateFns js/dateFns)

(defn format
  "Returns a string representation of the Instant
  instace with optional `fmt` format parameter.

  You can use `:iso` and `:unix` shortcuts as
  format parameter.

  You can read more about format tokens here:
  http://momentjs.com/docs/#/displaying/format/
  "
  ([v] (format v :iso))
  ([v fmt]
   (case fmt
     :offset (.getTime v)
     :iso (.format dateFns v)
     (.format dateFns v fmt))))

(defn now
  "Return the current Instant."
  []
  (js/Date.))

(defn timeago
  [v]
  {:pre [(inst? v)]}
  (.distanceInWordsToNow dateFns v))

(defn parse
  [v]
  {:pre [(number? v)]}
  (.parse dateFns v))
