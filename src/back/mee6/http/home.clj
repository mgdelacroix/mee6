(ns mee6.http.home
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [hiccup.page :refer [html5]]
            [cuerdas.core :as str]
            [mee6.engine :as engine]
            [mee6.config :as cfg]
            [mee6.database :refer [state]]
            [mee6.http.common :refer [html-head body-header]]))

(defn body-content-item
  [{:keys [id name host cron] :as check} {:keys [status updated-at] :as data}]
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
  [state]
  (let [total-count (count engine/checks)
        matches-status? #(= (get-in state [:checks (:id %1) :status]) %2)
        ok-count (count (filter #(matches-status? % :green) engine/checks))
        ko-count (count (filter #(matches-status? % :red) engine/checks))
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
  [state]
  [:div#main-content.content
   [:section#items
    (body-content-summary state)
    [:h2 "INSTANCES"]
    [:ul.list.items
     (for [{:keys [id] :as item} engine/checks]
       (->> (get-in state [:checks id])
            (body-content-item item)))]]])

(defn- render-page
  [state]
  (html5
   (html-head :title "Mee6")
   [:body
    (body-header state)
    (body-content state)]))

(defn handler
  [request]
  {:body (render-page @state)
   :status 200})
