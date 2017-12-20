(ns mee6.ui.detail
  (:require [rumext.core :as mx :include-macros true]
            [cuerdas.core :as str :include-macros true]
            [mee6.util.router :as rt]
            [mee6.ui.common :as common]))

(mx/defc check-item
  {:mixins [mx/static]}
  [{:keys [id name cron host status updatedAt] :as check}]
  [:li.item {:class (case status
                      "green" "item-ok"
                      "red" "item-ko"
                      "item-disabled")}
   [:a {:href (rt/route-for :detail {:id id})}
    [:div.item-content (str/istr "~{host} :: ~{name}")]
    [:ul.meta
     [:li [:strong "cron: "] cron]
     [:li [:strong "last run: "] (str updatedAt)]]]])

(mx/defc check
  {:mixins [mx/static]}
  [{:keys [config output error] :as check}]
  [:div
   [:ul.list.items
    (check-item check)]
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
