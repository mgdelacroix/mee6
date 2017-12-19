(ns mee6.config
  (:refer-clojure :exclude [load])
  (:require [mount.core :as mount :refer [defstate]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [datoteka.core :as fs]
            [environ.core :refer [env]]
            [yaml.core :as yaml]
            [mee6.exceptions :as exc]))

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

(s/def :check/notify (s/coll-of string? :kind vector? :min-count 0))
(s/def :config/check (s/keys :req-un [:check/hosts
                                      :check/cron
                                      :check/module]
                             :opt-un [:check/notify]))

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
(s/def :config/http
  (s/keys :req-un [:http/port :http/users]))

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

(defn load
  []
  (-> (get env :mee6-config "resources/config.yml")
      (yaml/from-file)
      (keywordize-keys)
      (validate)))

(defstate config
  :start (load))

