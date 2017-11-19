(ns mee6.database
  "Application local running state."
  (:require [clojure.java.io :as io]
            [mount.core :refer [defstate]]
            [datoteka.core :as fs]
            [mee6.logging :as log]
            [mee6.transit :as t]
            [mee6.config :as cfg]))

;; --- State Ref

(defonce state (atom {}))

;; --- Persistence Impl

(defn- read-state
  [src]
  {:pre [(fs/path? src)]}
  (when (and (fs/exists? src)
             (not (fs/directory? src)))
    (some-> (fs/slurp-bytes src)
            (t/decode {:type :msgpack}))))

(defn- persist-state
  [dst data]
  {:pre [(fs/path? dst)]}
  (let [data (t/encode data {:type :msgpack})
        tmp (fs/path (str dst ".tmp"))]
    (with-open [dst (io/output-stream tmp)]
      (.write dst data))
    (fs/move tmp dst)))

(defn- load-initial-state
  [src]
  {:pre [(fs/path? src)]}
  (try
    (if-let [content (read-state src)]
      (reset! state content)
      (reset! state {}))
    (catch clojure.lang.ExceptionInfo e
      (log/err "Can't load initial state, seems corrupted." e))))

(defn- initialize-persistence
  [dst]
  {:pre [(fs/path? dst)]}
  (add-watch state ::watcher (fn [_ _ _ data]
                               (persist-state dst data)))
  (reify java.lang.AutoCloseable
    (close [_]
      (remove-watch state ::watcher))))

(defn- start-store
  [{:keys [database] :as config}]
  {:pre [(string? database)]}
  (let [database (-> database fs/path fs/normalize)]
    (log/inf "Initializing storage in:" database)
    (load-initial-state database)
    (initialize-persistence database)))

(defn- stop-store
  [persistence]
  (.close persistence)
  (reset! state {}))

(defstate store
  :start (start-store cfg/config)
  :stop (stop-store store))
