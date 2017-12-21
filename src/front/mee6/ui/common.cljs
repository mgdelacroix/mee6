(ns mee6.ui.common
  (:require [rumext.core :as mx :include-macros true]
            [cuerdas.core :as str :include-macros true]
            [mee6.events :as ev]
            [mee6.notifications :as ntf]
            [mee6.store :as st]
            [mee6.util.router :as rt]
            [mee6.util.dom :as dom]))

(mx/defc header
  {:mixins [mx/static]}
  []
  (letfn [(on-logout [event]
            (dom/prevent-default event)
            (st/emit! (ev/->Logout)))]
    [:header
     [:div.logo {:on-click #(st/emit! (rt/navigate :home))} "Mee6"]
     [:div.tagline {:on-click ntf/request-permission}
      "I'm Mr. Mee6! Look at me!"]
     [:div.logout
      [:a {:on-click on-logout} "Logout"]]]))

(mx/defc summary
  {:mixins [mx/static]}
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
