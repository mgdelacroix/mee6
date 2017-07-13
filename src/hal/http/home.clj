(ns hal.http.home
  (:require [clojure.java.io :as io]
            [hiccup.page :refer [html5]]))

(declare body-header)
(declare body-content)
(declare body-content-summary)

(defn html-head
  []
  [:head
   [:meta {:http-equiv "Content-Type"
           :content "text/html; charset=UTF-8"}]
   [:title "Mee6"]
   [:style {:media "screen"}
    (slurp (io/resource "styles.css"))]])

(defn html-body
  []
  [:body
   (body-header)
   (body-content)])

(defn body-header
  []
  [:header
   [:span.logo]
   [:h1.logo-name "Mee6"]
   [:p.tagline "Hola! Soy el señor Meeseeks! Miradme!"]])



(defn body-content
  []
  [:div.content
   [:section#items
    (body-content-summary)
    [:h2 "INSTANCIAS"]
    [:ul.list.items
     [:li.item.item-ok
      [:div.item-content "DB :: Disk usage on /dev/sda1"]
      [:ul.meta
       [:li [:strong "cron:"] "*/5 * * * *"]
       [:li [:strong "last run:"] "00:00:00"]]]
     [:li.item.item-ko
      [:div.item-content "DB :: Disk usage on /dev/sda1"]
      [:ul.meta
       [:li [:strong "cron:"] "*/5 * * * *"]
       [:li [:strong "last run:"] "00:00:00"]]]
     [:li.item.item-disabled
      [:div.item-content "DB :: Disk usage on /dev/sda1"]
      [:ul.meta
       [:li [:strong "cron:"] "*/5 * * * *"]
       [:li [:strong "last run:"] "00:00:00"]]]]]])

(defn body-content-summary
  []
  [:div.summary
   [:div.progress
    [:div.bar.bar-ok {:style "width: 33%"}]
    [:div.bar.bar-ko {:style "width: 33%"}]
    [:div.bar.bar-disabled {:style "width: 34%"}]]
   [:ul.data
    [:li.data-ok
     [:span.value "33"]
     [:span.label "Working"]]
    [:li.data-ko
     [:span.value "33"]
     [:span.label "Failing"]]
    [:li.data-ko
     [:span.value "34"]
     [:span.label "Disabled"]]]])

(defn- page
  []
  (html5
   (html-head)
   (html-body)))

(defn handler
  [request]
  {:body (page)
   :status 200})

