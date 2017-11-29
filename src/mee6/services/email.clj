(ns mee6.services.email
  (:require [clojure.core.async :as a]
            [cuerdas.core :as str]
            [hiccup.core :refer [html]]
            [mee6.config :as cfg]
            [mee6.logging :as log]
            [mee6.services :as sv]
            [mount.core :refer [defstate]]
            [postal.core :as postal]
            [yaml.core :as yaml]))

(declare smtp-send!)
(declare console-send!)
(declare build-email)

(defn- start-rcvloop!
  [config in out]
  (a/go-loop []
    (when-let [payload (a/<! in)]
      (let [email (build-email config payload)]
        (a/>! out email)
        (recur)))))

(defn- start-sndloop!
  [config in]
  (let [params (select-keys config [:mode :host :user :pass :ssl :tls :port])]
    (a/go-loop []
      (when-let [email (a/<! in)]
        (case (:mode params)
          "smtp" (a/<! (smtp-send! params email))
          "console" (a/<! (console-send! params email))
          "local" (log/err "not implemented `local` send method"))
        (recur)))))

;; --- Entry Point

(defn- initialize
  [config]
  (let [inbox (a/chan 256 (map :payload))
        outbox (a/chan 256)]
    (sv/sub! :notifications/email inbox)
    (start-rcvloop! config inbox outbox)
    (start-sndloop! config outbox)
    {::service true
     ::inbox inbox
     ::outbox outbox}))

(defn- start
  [config]
  (when-let [config (:email config)]
    (log/inf "starting email service")
    (initialize config)))

(defn- stop
  [instance]
  (when (::service instance)
    (log/inf "stopping email service")
    (a/close! (::inbox instance))
    (a/close! (::outbox instance))
    nil))

(defstate instance
  :start (start cfg/config)
  :stop (stop instance))

;; --- Email Sending

(defn- smtp-send!
  [params email]
  (a/thread
    (try
      (postal/send-message params email)
      (catch Throwable e
        (log/err "error on sending email" e)))))

(defn- console-send!
  [params {:keys [to subject] :as email}]
  (a/go
    (log/inf "========== start email ==========")
    (log/inf " To:" to)
    (log/inf " Subject: " subject)
    (log/inf "========== end email ==========")))

;; --- Email Building

(defn- dump-yaml
  [value]
  (yaml/generate-string value :dumper-options {:flow-style :block}))

(defn- build-email-body
  [{:keys [check local error current-status previous-status] :as payload}]
  (html
   [:div.title
    [:h1 (case current-status :green "SUCCESS" :red "FAILED" :grey "ERROR")]
    [:div.data
     [:ul
      [:li [:strong "host:"] (get-in check [:host :uri])]
      [:li [:strong "name:"] (get-in check [:name])]
      [:li
       [:strong "result:"]
       (if error
         [:pre (:stacktrace error)]
         [:pre (dump-yaml local)])]]]]))

(defn- build-email-subject
  [{:keys [check local error current-status previous-status] :as payload}]
  (let [hostname (get-in check [:host :id])
        checkname (get check :name)]
    (str/istr "[~{previous-status} -> ~{current-status}] ~{hostname} :: ~{checkname}")))

(defn- build-email
  [config {:keys [notify-group] :as payload}]
  (try
    (let [body (build-email-body payload)
          subject (build-email-subject payload)]
      {:from (:from config)
       :to (:emails notify-group)
       :subject subject
       :body [{:type "text/html"
               :content body}]})
    (catch Throwable e
      (log/err "Unexpected error on building email" e)
      nil)))


