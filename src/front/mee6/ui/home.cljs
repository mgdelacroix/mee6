(ns mee6.ui.home
  (:require [rumext.core :as mx :include-macros true]
            [mee6.ui.common :as common]))

(mx/defc main
  {:mixins [mx/static]}
  [state]
  (let [checks (:checks state)]
    [:div
     [:h2 "INSTANCES"]
     [:ul.list.items
      (mx/doseq [id (keys checks)]
        (-> (common/body-content-item (get checks id))
            (mx/with-key id)))]]))
