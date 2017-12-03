(ns mee6.services
  "A core namespace for message bus abstraction"
  (:require [mount.core :refer [defstate]]
            [clojure.core.async :as a]))

;; --- State Management

(defstate bus
  :start (a/chan)
  :stop (a/close! bus))

(defstate pub
  :start (a/pub bus :topic))

;; --- Public API

(defn sub!
  ([topic]
   (sub! topic (a/chan 16)))
  ([topic ch]
   (a/sub pub topic ch)
   ch))

(defn unsub!
  ([topic]
   (a/unsub-all pub topic))
  ([topic ch]
   (a/unsub pub topic ch)))

(defn emit!
  [topic payload]
  (let [msg {:topic topic
             :payload payload}]
    (a/go
      (a/>! bus msg))))

(defn service?
  [v]
  (boolean (::service v)))
