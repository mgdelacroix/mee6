(ns mee6.ui
  (:require [rumext.core :as mx :include-macros true]
            [mee6.util.dom :as dom]
            [mee6.store :as st]))

(mx/defc home
  []
  [:p "Hello home"])

(mx/defc detail
  []
  [:p "Hello detail"])

(mx/defc app
  {:mixins [mx/reactive]}
  []
  (let [{:keys [route]} (mx/react st/state)]
    (case (:id route)
      :home (home)
      :detail (detail)
      (home))))

(defn init
  []
  (mx/mount (app) (dom/get-element "app")))
