(ns mee6.notifications
  (:require [mee6.logging :as log]
            [mee6.config :as cfg]
            [hiccup.core :refer [html]]
            [postal.core :as postal])
  (:refer-clojure :exclude [send]))

(defn- get-mail-config
  [{:keys [mail]}]
  (select-keys mail [:mode :from :host :user :pass :ssl :tls :port]))

(defn- format-body
  [checkname status hostname result ctx]
  (html [:div.title
         (case status
           :green
           [:h1 "SUCCESS"]
           :red
           [:h1 "FAILED"]
           :grey
           [:h1 "ERROR"])
         [:div.data
          [:ul
           [:li [:strong "host"] (str " :: " hostname)]
           [:li [:strong "status"] (str " :: " (name status))]
           [:li [:strong "name"] (str " :: " checkname)]
           [:li [:strong "result"] (str " :: " result)]
           [:li [:strong "ctx"] (str " :: " ctx)]]]]))

(defn- format-subject
  [checkname status hostname]
  (str "[" (.toUpperCase (name status)) "] " hostname " :: " checkname))

(defn- build-mail
  [{:keys [name host] :as ctx} status result to {:keys [from] :as mailcfg}]
  (let [subject (format-subject name status (:hostname host))
        body (format-body name status (:hostname host) result ctx)]
    {:from from
     :to to
     :subject subject
     :body [{:type "text/html"
             :content body}]}))

(defn- send-mail
  [ctx status result to]
  (let [mailcfg (get-mail-config cfg/config)
        mail (build-mail ctx status result to mailcfg)]
    (postal/send-message mailcfg mail)))

(defn- send-email-to-console
  [{:keys [name host] :as ctx} status result to]
  (log/inf "========== start email ==========")
  (log/inf " TO:" to)
  (log/inf " HOST:" (:hostname host))
  (log/inf " STATUS:" status)
  (log/inf " CHECK:" name)
  (log/inf " RESULT:" result)
  (log/inf " CTX:" ctx)
  (log/inf "========== end email ==========")
  {:error :SUCCESS})

(defn send
  [ctx status result to]
  (let [mode (get-in cfg/config [:mail :mode])
        res (if (= mode "console")
                 (send-email-to-console ctx status result to)
                 (send-mail ctx status result to))]
    (if (= (:error res) :SUCCESS)
      (log/inf "Message to" to "sent successfully.")
      (log/err "Message to" to "failed with:" (:message res)))))

(defn send-all
  [{:keys [notify] :as ctx} status result]
  (run! #(send ctx status result %) (:emails notify)))

(defn send-exception-all
  [{:keys [notify] :as ctx} result]
  (run! #(send ctx :grey result %) (:emails notify)))
