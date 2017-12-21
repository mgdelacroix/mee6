(ns mee6.util.time
  (:refer-clojure :exclude [< >])
  (:require [cognitect.transit :as t])
  (:import java.time.Instant))

;; --- Transit & EDN adapters

(def ^:private write-handler
  (t/write-handler
   (constantly "m")
   (fn [v] (str (.toEpochMilli v)))))

(def ^:private read-handler
  (t/read-handler
   (fn [v] (-> (Long/parseLong v)
               (Instant/ofEpochMilli)))))

(def +read-handlers+
  {"m" read-handler})

(def +write-handlers+
  {Instant write-handler})

(defmethod print-method Instant
  [mv ^java.io.Writer writer]
  (.write writer (str "#instant \"" (.toString mv) "\"")))

(defmethod print-dup Instant [o w]
  (print-method o w))

;; --- Public Api

(defn to-epoch-milli
  [^Instant v]
  (.toEpochMilli v))

(defn from-epoch-milli
  [^long v]
  (Instant/ofEpochMilli v))

(defn from-string
  [s]
  {:pre [(string? s)]}
  (Instant/parse s))

(defn instant?
  [v]
  (instance? Instant v))

(defn now
  []
  (Instant/now))

(defn plus
  [inst & {:keys [days hours millis seconds nanos years]}]
  {:pre [(instant? inst)]}
  (cond-> inst
    years (.plusSeconds (* years 365 86400))
    days (.plusSeconds (* days 86400))
    seconds (.plusSeconds seconds)
    hours (.plusSeconds (* hours 3600))
    millis (.plusMillis millis)
    nanos (.plusNanos nanos)))

(defn minus
  [inst & {:keys [days hours millis seconds nanos years]}]
  {:pre [(instant? inst)]}
  (cond-> inst
    years (.minusSeconds (* years 365 86400))
    days (.minusSeconds (* days 86400))
    seconds (.minusSeconds seconds)
    hours (.minusSeconds (* hours 3600))
    millis (.minusMillis millis)
    nanos (.minusNanos nanos)))

(defn >
  [^Instant a ^Instant b]
  {:pre [(instant? a) (instant? b)]}
  (pos? (.compareTo a b)))

(defn <
  [^Instant a ^Instant b]
  {:pre [(instant? a) (instant? b)]}
  (neg? (.compareTo a b)))
