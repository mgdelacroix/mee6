(ns hal.http.home
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [hiccup.page :refer [html5]]
            [cuerdas.core :as str]
            [hal.state :refer [state]]
            [hal.http.common :refer [html-head body-header]]))

(defn body-content-item
  [{:keys [id name host cron] :as check} {:keys [status updated-at] :as result}]
  [:li.item {:class (case status
                      :green "item-ok"
                      :red "item-ko"
                      "item-disabled")}
   [:a {:href (str/istr "/detail/~{id}")}
    [:div.item-content (str/istr "~(:hostname host) :: ~{name}")]
    [:ul.meta
     [:li [:strong "cron: "] cron]
     [:li [:strong "last run: "] (str updated-at)]]]])

(defn body-content-summary
  [{:keys [checks results] :as state}]
  (let [total-count (count checks)
        matches-status? #(= (get-in results [(:id %1) :status]) %2)
        ok-count (count (filter #(matches-status? % :green) checks))
        ko-count (count (filter #(matches-status? % :red) checks))
        ds-count (- total-count ko-count ok-count)
        ok-pcent (quot (* ok-count 100) total-count)
        ko-pcent (quot (* ko-count 100) total-count)
        ds-pcent (- 100 ok-pcent ko-pcent)]
    [:div.summary
     [:div.progress
      [:div.bar.bar-ok {:style (str/istr "width: ~{ok-pcent}%")}]
      [:div.bar.bar-ko {:style (str/istr "width: ~{ko-pcent}%")}]
      [:div.bar.bar-disabled {:style (str/istr "width: ~{ds-pcent}%")}]]
     [:ul.data
      [:li.data-ok
       [:span.value ok-count]
       [:span.label "Working"]]
      [:li.data-ko
       [:span.value ko-count]
       [:span.label "Failing"]]
      [:li.data-disabled
       [:span.value ds-count]
       [:span.label "Disabled"]]]]))

(defn body-content
  [{:keys [checks results] :as state}]
  [:div.content
   [:section#items
    (body-content-summary state)
    [:h2 "INSTANCIAS"]
    [:ul.list.items
     (for [item checks
           :let [result (get results (:id item))]]
       (body-content-item item result))]]])

(defn- page
  [state]
  (html5
   (html-head :title "Mee6")
   [:body
    (body-header state)
    (body-content state)]))

(defn handler
  [request]
  (let [state (deref state)]
    {:body (page state)
     :status 200}))

