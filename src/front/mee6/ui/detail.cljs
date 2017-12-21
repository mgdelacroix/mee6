(ns mee6.ui.detail
  (:require [rumext.core :as mx :include-macros true]
            [cuerdas.core :as str :include-macros true]
            [mee6.store :as st]
            [mee6.util.router :as rt]
            [mee6.util.time :as tm]
            [mee6.ui.common :as common]))

(mx/defc header
  {:mixins [mx/static]}
  [{:keys [id name cron host status updated-at] :as check}]
  [:section.detail-header
   {:on-click #(st/emit! (rt/navigate :detail {:id id}))
    :class (case status
             "green" "item-ok"
             "red" "item-ko"
             "item-disabled")}
   [:div.item-title (str/istr "~{host} :: ~{name}")]
   [:div.item-meta
    [:div.cron [:strong "cron: "] cron]
    [:div.lastrun
     [:strong "last run: "]
     (if updated-at
       (tm/format (tm/parse updated-at) "dddd, MMMM Do YYYY, h:mm:ss a")
       "---")]]])

(mx/defc main
  {:mixins [mx/static]}
  [{:keys [config output error] :as check}]
  [:section.detail
   (header check)
   (if-not (empty? output)
     [:div
      [:h3 "Latest output:"]
      [:section.code
       [:pre output]]])

   (when-let [{:keys [stdout stderr message]} error]
     [:div
      [:h3 "Error:"]
      (let [connection-warning (if (every? #(empty? %) [stdout stderr])
                                 "Please check the ssh connection to the host.")]
        [:section.code
         [:pre (str/join " " [message connection-warning])]])])

   [:h3 "Check configuration:"]
   [:section.code
    [:pre config]]])
