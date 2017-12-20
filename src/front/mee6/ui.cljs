(ns mee6.ui
  (:require [rumext.core :as mx :include-macros true]
            [beicon.core :as rx]
            [mee6.util.dom :as dom]
            [mee6.store :as st]
            [mee6.events :as ev]
            [mee6.util.router :as rt]
            [mee6.util.timers :as ts]
            [mee6.ui.common :as common]
            [mee6.ui.detail :as detail]
            [mee6.ui.home :as home]
            [mee6.ui.login :as login]))

(defn- on-error
  "A default error handler."
  [{:keys [type] :as error}]
  (js/console.error "on-error:" (pr-str error))
  (js/console.error (.-stack error))
  (cond
    (= type "not-authorized")
    (ts/schedule 100 #(st/emit! (ev/->Logout)))

    ;; Something else
    :else nil))

(set! st/*on-error* on-error)

;; --- App (Component)

(def +refresh-time-milis+ 5000)

(defn content-will-mount
  [own]
  (let [retrieve #(st/emit! (ev/->RetrieveChecks))]
    (retrieve)
    (->> (ts/interval +refresh-time-milis+ retrieve)
         (assoc own ::retrieve-interval))))

(defn content-will-unmount
  [own]
  (rx/cancel! (::retrieve-interval own))
  (dissoc own ::retrieve-interval))

(mx/defc content
  {:will-mount content-will-mount
   :will-unmount content-will-unmount}
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
  {:mixins [mx/reactive mx/static]}
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
