(ns mee6.ui.home
  (:require [rumext.core :as mx :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cuerdas.core :as str :include-macros true]
            [mee6.store :as st]
            [mee6.events :as ev]
            [cuerdas.core :as str]))

(mx/defc body-content-item
  [{:keys [id name cron host status updatedAt] :as check}]
  [:li.item {:class (case status
                      "green" "item-ok"
                      "red" "item-ko"
                      "item-disabled")}
   [:a {:href (str/istr "/detail/~{id}")}
    [:div.item-content (str/istr "~{host} :: ~{name}")]
    [:ul.meta
     [:li [:strong "cron: "] cron]
     [:li [:strong "last run: "] (str updatedAt)]]]])

(mx/defc body-content-summary
  [checks]
  (let [total-count (count checks)
        matches-status? #(= (:status %1) %2)
        ok-count (count (filter #(matches-status? % "green") (vals checks)))
        ko-count (count (filter #(matches-status? % "red") (vals checks)))
        ds-count (- total-count ko-count ok-count)
        ok-pcent (quot (* ok-count 100) total-count)
        ko-pcent (quot (* ko-count 100) total-count)
        ds-pcent (- 100 ok-pcent ko-pcent)]
    [:div.summary
     [:div.progress
      [:div.bar.bar-ok {:style {:width (str/istr "~{ok-pcent}%")}}]
      [:div.bar.bar-ko {:style {:width (str/istr "~{ko-pcent}%")}}]
      [:div.bar.bar-disabled {:style {:width (str/istr "~{ds-pcent}%")}}]]
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

(mx/defc main
  {:will-mount #(st/emit! (ev/->RetrieveChecks))}
  [state]
  (let [checks (:checks state)]
    [:div#main-content.content
     [:section#items
      (body-content-summary (:checks state))
      [:h2 "INSTANCES"]
      [:ul.list.items
       (for [id (keys checks)]
         (-> (body-content-item (get checks id))
             (mx/with-key id)))]]]))
