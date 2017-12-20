(ns mee6.ui.home
  (:require [rumext.core :as mx :include-macros true]
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

(mx/defc main
  {:mixins [mx/static]}
  [state]
  (let [checks (:checks state)]
    [:section.home
     [:section.home-sidebar
      [:h2 "Tags"]
      [:div.sidebar-items
       [:div.sidebar-item "production"]
       [:div.sidebar-item "localhost"]
       [:div.sidebar-item "testing"]]]
     [:section.home-content
      [:h2 "INSTANCES"]
      [:ul.list.items
       (mx/doseq [id (keys checks)]
         (-> (check-item (get checks id))
             (mx/with-key id)))]]]))
