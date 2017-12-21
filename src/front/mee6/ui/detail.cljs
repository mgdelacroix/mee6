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

(defn build-error-message
  [{:keys [message hint output stdout stderr type] :as error}]
  (let [output (if-not (empty? output) (str "---\n" output))]
    (if (and (= type "execution-error")
             (every? #(empty? %) [stdout stderr]))
      (str/join " " [message "Please check the ssh connection to the host."])
      (str/join "\n\n" (filter #(not (empty? %)) [message hint output stderr stdout])))))

(mx/defc main
  {:mixins [mx/static]}
  [{:keys [config output error] :as check}]
  [:section.detail
   (header check)
   (if (and (empty? error)
            (not (empty? output)))
     [:div
      [:h3 "Latest output:"]
      [:section.code
       [:pre output]]])

   (when-let [{:keys [stdout stderr message]} error]
     [:div
      [:h3 "Error:"]
      [:section.code
       [:pre (build-error-message error)]]])

   [:h3 "Check configuration:"]
   [:section.code
    [:pre config]]])
