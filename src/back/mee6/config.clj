(ns mee6.config
  (:refer-clojure :exclude [load])
  (:require [mount.core :as mount :refer [defstate]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [datoteka.core :as fs]
            [environ.core :refer [env]]
            [mee6.exceptions :as exc]
            [mee6.util.logging :as log]
            [mee6.util.yaml :as yaml]))

(def ^:dynamic *config-path* "resources/config.yml")

;; --- Configuration file spec.

(s/def :database/path string?)
(s/def :database/debounce int?)

(s/def :config/database
  (s/keys :req-un [:database/path]
          :opt-un [:database/debounce]))

(defn path-conformer
  [v]
  (if (string? v)
    (fs/path v)
    ::s/invalid))

(s/def :config/modules-item (s/conformer path-conformer))
(s/def :config/modules (s/coll-of :config/modules-item :kind vector? :min-count 1))

(s/def :host/uri string?)
(s/def :host/key string?)

(s/def :config/log-level #{"info" "debug" "error"})
(s/def :config/host (s/keys :req-un [:host/uri]
                            :opt-un [:host/key]))

(s/def :config/hosts (s/map-of keyword? :config/host :min-count 1))

(s/def :check/hosts (s/coll-of string? :kind vector? :min-count 1))
(s/def :check/cron string?)
(s/def :check/module string?)
(s/def :check/tags (s/coll-of string? :kind vector? :min-count 1))

(s/def :check/notify (s/coll-of string? :kind vector? :min-count 0))
(s/def :config/check (s/keys :req-un [:check/hosts
                                      :check/cron
                                      :check/module]
                             :opt-un [:check/notify
                                      :check/tags]))

(s/def :config/checks (s/coll-of :config/check :kind vector? :min-count 1))
(s/def :config/notify (s/map-of keyword? map?))


(s/def :email/from string?)
(s/def :email/mode string?)
(s/def :email/host string?)
(s/def :email/user string?)
(s/def :email/pass string?)
(s/def :email/ssl boolean?)
(s/def :email/tls boolean?)
(s/def :email/port int?)

(s/def :http/users (s/map-of keyword? string?))

(s/def :http/port int?)
(s/def :http/graphiql boolean?)
(s/def :config/http
  (s/keys :req-un [:http/port :http/users]
          :opt-un [:http/graphiql]))

(s/def :config/email
  (s/keys :req-un [:email/from :email/mode]
          :opt-un [:email/user :email/pass
                   :email/ssl :email/tls :email/port]))

(s/def ::config (s/keys :req-un [:config/hosts
                                 :config/checks]
                        :opt-un [:config/database
                                 :config/log-level
                                 :config/notify
                                 :config/modules
                                 :config/email
                                 :config/http]))

;; --- Configuration file validation and loading

(defn- validate
  [data]
  (let [config (s/conform ::config data)]
    (when (= config ::s/invalid)
      (exc/raise :message "Invalid configuration."
                 :type :validation
                 :explain (s/explain-str ::config data)))
    config))

(defn- reconfigure-logging!
  [{:keys [log-level]}]
  (when log-level
    (log/configure! {:level (keyword log-level)})))

(def ^:private default-logging-config
  {:level :info
   :timestamp-opts {:pattern "yyyy-MM-dd HH:mm:ss"
                    :locale :jvm-default
                    :timezone :utc}})


(defn load
  []
  ;; Set initial logging configuration
  (log/configure! default-logging-config)

  ;; Load config file
  (let [path (get env :mee6-config *config-path*)
        cfg  (-> (yaml/decode-from-file path)
                 (keywordize-keys)
                 (validate))]

    (log/inf "Configuration loaded from path:" path)
    ;; (when (nil? cfg)
    ;;   (log/err "Configuration is empty or file does not exists!"))

    ;; Reconfigure logging with loaded configuration
    (reconfigure-logging! cfg)

    cfg))

(defstate config
  :start (load))
