(ns mee6.ui
  (:require [rumext.core :as mx :include-macros true]
            [mee6.util.dom :as dom]
            [mee6.store :as st]
            [mee6.events :as ev]
            [mee6.util.router :as rt]
            [mee6.ui.common :as common]
            [mee6.ui.home :as home]
            [mee6.ui.detail :as detail]))

(defn app-will-mount
  [own]
  (st/emit! (ev/->RetrieveChecks))
  own)

(mx/defc app
  {:will-mount app-will-mount
   :mixins [mx/reactive]}
  []
  (let [{:keys [route checks] :as state} (mx/react st/state)]
    [:div
     (common/header)
     [:div#main-content.content
      [:section#items
       (common/body-content-summary checks)
       (case (:id route)
         :home (home/main state)
         :detail (let [id (get-in route [:params :id])
                       check (get checks id)]
                   (detail/check check))
         (home/main))]]]))

(def ^:private routes
  [["/" :home]
   ["/detail/:id" :detail]])

(defn init
  []
  (rt/init st/store routes {:default :home})
  (mx/mount (app) (dom/get-element "app")))
