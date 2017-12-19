(ns mee6.ui
  (:require [rumext.core :as mx :include-macros true]
            [mee6.util.dom :as dom]
            [mee6.store :as st]
            [mee6.events :as ev]
            [mee6.util.router :as rt]
            [mee6.ui.common :as common]
            [mee6.ui.detail :as detail]
            [mee6.ui.home :as home]
            [mee6.ui.login :as login]))

(defn content-will-mount
  [own]
  (st/emit! (ev/->RetrieveChecks))
  own)

(mx/defc content
  {:will-mount content-will-mount}
  [{:keys [route checks] :as state}]
  [:div#main-content.content
   [:section#items
    (common/body-content-summary checks)
    (case (:id route)
      :home (home/main state)
      :detail (let [id (get-in route [:params :id])
                    check (get checks id)]
                (detail/check check))
      (home/main))]])

(mx/defc app
  {:mixins [mx/reactive]}
  []
  (let [{:keys [route] :as state} (mx/react st/state)]
    (if (= (:id route) :login)
      (login/main)
      [:div
       (common/header)
       (content state)])))

(def ^:private routes
  [["/" :home]
   ["/detail/:id" :detail]
   ["/login" :login]])

(defn init
  []
  (rt/init st/store routes {:default :home})
  (mx/mount (app) (dom/get-element "app")))
