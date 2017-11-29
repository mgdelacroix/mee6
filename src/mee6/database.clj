(ns mee6.database
  "Application local running state."
  (:require [clojure.core.async :as a]
            [clojure.java.io :as io]
            [mount.core :refer [defstate]]
            [datoteka.core :as fs]
            [mee6.logging :as log]
            [mee6.transit :as t]
            [mee6.config :as cfg]))

;; --- State Ref

(defonce state (atom {}))

;; --- Helpers

(defn- debounce
  "A debounce implemented using core.async channels. Used for debouce
  the state persistence into the filesystem."
  [in ms]
  (let [out (a/chan)]
    (a/go-loop [status ::wait
                lastval ::empty
                timeout (a/timeout ms)]
      (case status
        ::wait
        (let [[newval ch] (a/alts! [in timeout])]
          (cond
            (identical? ch timeout)
            (recur ::emmit lastval nil)

            (identical? ch in)
            (recur status newval timeout)))

        ::emmit
        (cond
          (nil? lastval)
          (a/close! out)

          (= lastval ::empty)
          (recur ::wait lastval (a/timeout ms))

          :else
          (when (a/>! out lastval)
            (recur ::wait ::empty (a/timeout ms))))))
    out))

;; --- Persistence Impl

(defn- read-state
  "Read the state from the filesystem."
  [src]
  {:pre [(fs/path? src)]}
  (when (and (fs/exists? src)
             (not (fs/directory? src)))
    (some-> (fs/slurp-bytes src)
            (t/decode {:type :msgpack}))))

(defn- persist-state!
  "Persist the state into the filesystem."
  [dst data]
  {:pre [(fs/path? dst)]}
  (a/thread
    (try
      (let [data (t/encode data {:type :msgpack})
            tmp (fs/path (str dst ".tmp"))]
        (with-open [dst (io/output-stream tmp)]
          (.write dst data))
        (fs/move tmp dst))
      (catch Throwable e
        (log/err "Error on persisting state." e)))))

(defn- load-initial-state!
  [src]
  {:pre [(fs/path? src)]}
  (try
    (if-let [content (read-state src)]
      (reset! state content)
      (reset! state {}))
    (catch Throwable e
      (log/err "Can't load initial state, seems corrupted." e))))

(declare debounce)

(defn- initialize
  [path interval]
  (let [inbox (a/chan (a/sliding-buffer 8))
        inbox' (debounce inbox interval)]

    ;; Load initial data
    (load-initial-state! path)

    ;; Add a watcher to the state
    (add-watch state ::watcher #(a/>!! inbox %4))

    ;; Start the persistence loop
    (a/go-loop []
      (let [val (a/<! inbox')]
        (if (nil? val)
          (log/inf "Persistence loop terminated.")
          (do
            (a/<! (persist-state! path val))
            (recur)))))

    {::store true
     ::inbox inbox}))

(defn- start
  [config]
  (when-let [{:keys [path debounce] :or {debounce 3000}} (:database config)]
    (let [path (-> path fs/path fs/normalize)]
      (log/inf "Initializing storage in:" path)
      (initialize path debounce))))

(defn- stop
  [store]
  (when (::store store)
    (a/close! (::inbox store))
    (remove-watch state ::watcher)))

(defstate store
  :start (start cfg/config)
  :stop (stop store))
