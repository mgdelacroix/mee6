(ns mee6.ui.home
  (:require [rumext.core :as mx :include-macros true]
            [mee6.events :as ev]
            [mee6.store :as st]
            [mee6.ui.common :as common]))

(mx/defc main
  {:will-mount #(st/emit! (ev/->RetrieveChecks))}
  [state]
  (let [checks (:checks state)]
    [:div
     [:h2 "INSTANCES"]
     [:ul.list.items
      (for [id (keys checks)]
        (-> (common/body-content-item (get checks id))
            (mx/with-key id)))]]))
