(ns mee6.ui.home
  (:require [rumext.core :as mx :include-macros true]
            [cuerdas.core :as str :include-macros true]
            [mee6.util.router :as rt]
            [mee6.store :as st]
            [mee6.ui.common :as common]
            [mee6.events :as ev]))

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

(defn toggle-selected-tag
  [tag e]
  (.preventDefault e)
  (st/emit! (ev/->UpdateSelectedTag tag)))

(mx/defc main
  {:mixins [mx/static]}
  [{:keys [checks selected-tag] :as state}]
  (let [tags (into #{} (mapcat :tags (vals checks)))
        class-if-selected #(when (= selected-tag %) "selected")
        check-has-tag? (fn [[k {:keys [tags]}]] ((set tags) selected-tag))
        filtered-checks (if selected-tag
                          (into {} (filter check-has-tag?) checks)
                          checks)]
    [:section.home
     [:section.home-sidebar
      [:h2 "Tags"]
      [:div.sidebar-items
       (for [tag tags]
         [:div.sidebar-item {:key tag
                             :on-click (partial toggle-selected-tag tag)
                             :class (class-if-selected tag)} tag])]]
     [:section.home-content
      [:h2 "INSTANCES"]
      [:ul.list.items
       (mx/doseq [id (keys filtered-checks)]
         (-> (check-item (get checks id))
             (mx/with-key id)))]]]))
