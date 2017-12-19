(ns mee6.ui
  (:require [rumext.core :as mx :include-macros true]
            [mee6.util.dom :as dom]
            [mee6.store :as st]
            [mee6.util.router :as rt]
            [mee6.ui.home :as home]))

(mx/defc detail
  []
  [:p "Hello detail"])

(mx/defc app
  {:mixins [mx/reactive]}
  []
  (let [{:keys [route] :as state} (mx/react st/state)]
    (case (:id route)
      :home (home/main state)
      :detail (detail)
      (home/main))))

(def ^:private routes
  [["/" :home]
   ["/detail/:id" :detail]])

(defn init
  []
  (rt/init st/store routes {:default :home})
  (mx/mount (app) (dom/get-element "app")))
