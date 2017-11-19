(ns mee6.transit
  (:require [cognitect.transit :as t]
            [mee6.time :as dt])
  (:import java.io.ByteArrayInputStream
           java.io.ByteArrayOutputStream))

;; --- Handlers

(def ^:private +reader-handlers+
  dt/+read-handlers+)

(def ^:private +write-handlers+
  dt/+write-handlers+)

;; --- Low Level Api

(defn reader
  ([istream]
   (reader istream nil))
  ([istream {:keys [type] :or {type :msgpack}}]
   (t/reader istream type {:handlers +reader-handlers+})))

(defn read!
  "Read value from streamed transit reader."
  [reader]
  (t/read reader))

(defn writer
  ([ostream]
   (writer ostream nil))
  ([ostream {:keys [type] :or {type :msgpack}}]
   (t/writer ostream type {:handlers +write-handlers+})))

(defn write!
  [writer data]
  (t/write writer data))

;; --- High Level Api

(defn decode
  ([data]
   (decode data nil))
  ([data opts]
   {:pre [(bytes? data)]}
   (with-open [input (ByteArrayInputStream. data)]
     (read! (reader input opts)))))

(defn encode
  ([data]
   (encode data nil))
  ([data opts]
   (with-open [out (ByteArrayOutputStream.)]
     (let [w (writer out opts)]
       (write! w data)
       (.toByteArray out)))))
