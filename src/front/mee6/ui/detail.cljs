(ns mee6.ui.detail
  (:require [rumext.core :as mx :include-macros true]
            [mee6.ui.common :as common]))

(mx/defc check
  [{:keys [name host cron status config output error updatedAt] :as check}]
  [:div
   [:ul.list.items
    (common/body-content-item check)]
   [:div
    [:h3 "Latest output:"]
    [:section.code
     [:pre (:output check)]]]

   (when-let [{:keys [err]} error]
     [:div
      [:h3 "Error:"]
      [:section.code
       [:pre err]]])

   [:h3 "Check configuration:"]
   [:section.code
    [:pre config]]])
