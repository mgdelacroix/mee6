(ns mee6.ui.login
  (:require [rumext.core :as mx :include-macros true]
            [mee6.util.dom :as dom]
            [mee6.events :as ev]
            [mee6.store :as st]))

(defn submit-login
  [e]
  (.preventDefault e)
  (let [username (dom/get-element-value "username")
        password (dom/get-element-value "password")]
    (println "ENVIANDO")
    (println "Username" username)
    (println "Password" password)
    (st/emit! (ev/->RetrieveLogin username password))))

(mx/defc main
  []
  [:div#login
   [:h2 "LOGIN"]
   [:input#username {:type "text"
                     :name "username"
                     :placeholder "username"}]
   [:input#password {:type "password"
                     :name "password"
                     :placeholder "password"}]
   [:input {:type "button"
            :name "submit"
            :value "Login"
            :on-click submit-login}]])
