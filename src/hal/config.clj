(ns hal.config
  (:refer-clojure :exclude [load])
  (:require [mount.core :as mount :refer [defstate]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [environ.core :refer [env]]
            [yaml.core :as yaml]
            [hal.exceptions :as exc]))

;; --- Configuration file spec.

(s/def :host/hostname string?)
(s/def :host/key string?)

(s/def :config/host (s/keys :req-un [:host/hostname]
                            :opt-un [:host/key]))

(s/def :config/hosts (s/map-of keyword? :config/host :min-count 1))

(s/def :check/hosts (s/coll-of string? :kind vector? :min-count 1))
(s/def :check/cron string?)
(s/def :check/module string?)
(s/def :check/notify string?)

(s/def :config/check (s/keys :req-un [:check/hosts
                                      :check/cron
                                      :check/module
                                      :check/notify]))

(s/def :config/checks (s/coll-of :config/check :kind vector? :min-count 1))

(s/def :notify/description string?)
(s/def :notify/emails (s/coll-of string? :kind vector? :min-count 1))

(s/def :config/single-notify (s/keys :req-un [:notify/description
                                              :notify/emails]))

(s/def :config/notify (s/map-of keyword? :config/single-notify))

(s/def :mail/from string?)
(s/def :mail/mode string?)
(s/def :mail/host string?)
(s/def :mail/user string?)
(s/def :mail/pass string?)
(s/def :mail/ssl boolean?)
(s/def :mail/tls boolean?)
(s/def :mail/port int?)

(s/def :config/mail
  (s/or :local
        (s/and (s/keys :req-un [:mail/from :mail/mode])
               #(#{"console" "local"} (:mode %)))
        :smtp
        (s/and (s/keys :req-un [:mail/from :mail/mode]
                       :opt-un [:mail/user :mail/pass :mail/ssl :mail/tls :mail/port])
               #(= (:mode %) "smtp")
               #(not (and (:ssl %)
                          (:tls %))))))

(s/def ::config (s/keys :req-un [:config/hosts
                                 :config/checks
                                 :config/notify
                                 :config/mail]))

;; --- Configuration file validation and loading

(defn- validate
  [data]
  (let [result (s/conform ::config data)]
    (when (= result ::s/invalid)
      (exc/raise :message "Invalid configuration."
                 :type :validation
                 :explain (s/explain-str ::config data)))

    result))

(defn load
  []
  (-> (get env :hal-config "resources/config.yml")
      (yaml/from-file)
      (keywordize-keys)
      (validate)))

(defstate config
  :start (load))
