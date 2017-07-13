(ns hal.http.common
  (:require [clojure.java.io :as io]
            [cuerdas.core :as str]))

(defn html-head
  [& {:keys [title]}]
  [:head
   [:meta {:http-equiv "Content-Type"
           :content "text/html; charset=UTF-8"}]
   [:title title]
   [:style {:media "screen"}

    (slurp (io/resource "styles.css"))]])

(defn body-header
  [state]
  [:header
   [:span.logo]
   [:h1.logo-name "Mee6"]
   [:p.tagline "Hola! Soy el señor Meeseeks! Miradme!"]])
