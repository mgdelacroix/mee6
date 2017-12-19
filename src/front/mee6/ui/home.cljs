(ns mee6.ui.home
  (:require [rumext.core :as mx :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cuerdas.core :as str :include-macros true]
            [mee6.store :as st]
            [mee6.events :as ev]))

(defn main-will-mount
  []
  (st/emit! (ev/->RetrieveChecks)))

(mx/defc main
  {:will-mount main-will-mount}
  []
  [:div "JELLO GUORLD"])
