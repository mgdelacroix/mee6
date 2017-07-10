(ns hal.config
  (:require [yaml.core :as yaml]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.spec.alpha :as s]))

;; {:hosts
;;  [{:name "db", :address "192.168.1.1", :port 22, :username "whoever"}],
;;  :checks
;;  [{:name "Disk usage on /dev/sda1",
;;    :hosts ["db" "web1" "web2"],
;;    :cron "5 * * * *",
;;    :module "disk_usage",
;;    :args {:device "/dev/sda1", :yellow_flag 80, :red_flag 95},
;;    :notify "sys"}],
;;  :notifications
;;  [{:name "sys",
;;    :description "The systems guys",
;;    :email ["john.doe@example.com" "jane.doe@example.com"]}]}

(s/def :host/name string?)
(s/def :host/address string?)
(s/def :host/port int?)
(s/def :host/username string?)

(s/def :config/host (s/keys :req-un [:host/name
                                     :host/address
                                     :host/port
                                     :host/username]))
(s/def :config/hosts (s/coll-of :config/host :kind vector? :min-count 1))

(s/def :check/hosts (s/coll-of string? :kind vector? :min-count 1))
(s/def :check/cron string?)
(s/def :check/module string?)
(s/def :check/args map?)
(s/def :check/notify string?)

(s/def :config/check (s/keys :req-un [:check/hosts
                                      :check/cron
                                      :check/module
                                      :check/args
                                      :check/notify]))

(s/def :config/checks (s/coll-of :config/check :kind vector? :min-count 1))

(s/def :notification/name string?)
(s/def :notification/description string?)
(s/def :notification/email (s/coll-of string? :kind vector? :min-count 1))

(s/def :config/notification (s/keys :req-un [:notification/name
                                             :notification/description
                                             :notification/email]))

(s/def :config/notifications (s/coll-of :config/notification :kind vector?))

(s/def ::config (s/keys :req-un [:config/hosts
                                 :config/checks
                                 :config/notifications]))

(defn validate
  [config]
  (let [result (s/conform ::config config)]
    (if (= result ::s/invalid)
      (do (println (s/explain-str ::config config))
          (System/exit 1))
      result)))

(defn parse
  [path]
  (-> path
      (yaml/from-file)
      (keywordize-keys)
      (validate)))
