(ns mee6.ui.login
  (:require [cljs.spec.alpha :as s :include-macros true]
            [rumext.core :as mx :include-macros true]
            [cuerdas.core :as str :include-macros true]
            [mee6.util.dom :as dom]
            [mee6.util.forms :as fm]
            [mee6.events :as ev]
            [mee6.store :as st]))

(s/def ::username ::fm/non-empty-string)
(s/def ::password ::fm/non-empty-string)
(s/def ::login-form
  (s/keys :req-un [::username ::password]))

(mx/defcs main
  {:mixins [mx/static (mx/local)]}
  [{:keys [rum/local] :as own}]
  (let [data   (:form @local)
        error? (:error @local)
        valid? (fm/valid? ::login-form data)]
    (letfn [(on-change [event]
              (swap! local assoc :error false)
              (let [target (dom/get-target event)
                    field (keyword (dom/get-name target))
                    value (dom/get-value target)]
                (swap! local update :form assoc field value)))

            (on-error []
              (swap! local assoc :error true))

            (on-submit [event]
              (dom/prevent-default event)
              (swap! local dissoc :error)
              (st/emit! (ev/->Login data on-error)))]
      [:section.login
       [:div.login-content
        [:h2 "LOGIN"]
        (when error?
          [:span.error-message "Invalid credentials"])
        [:form {:on-submit on-submit}
         [:div.field
          [:input {:type "text"
                   :value (get-in @local [:form :username] "")
                   :tab-index "1"
                   :on-change on-change
                   :name "username"
                   :placeholder "username"}]]
         [:div.field
          [:input {:type "password"
                   :value (get-in @local [:form :password] "")
                   :tab-index "2"
                   :on-change on-change
                   :name "password"
                   :placeholder "password"}]]
         [:div.submit
          [:button {:type "submit"
                           :tab-index "3"
                           :class (when-not valid? "btn-disabled")
                           :disabled (not valid?)
                           :name "button"}
           "Login"]]]]])))
