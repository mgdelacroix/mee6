(ns hal.http.detail
  (:require [clojure.java.io :as io]
            [hiccup.page :refer [html5]]
            [hal.http.home :refer [html-head body-header body-content-summary]]))


(declare body-content)

(defn html-body
  []
  [:body
   (body-header)
   (body-content)])

(defn body-content
  []
  [:div.content
   [:section#items
    (body-content-summary)
    [:h3 "Titulo 1"]

    [:section.code
     [:pre "DB :: Disk usage on /dev/sda1 lksdfjdlfsa jdklsjf kldsaj fldasjfldksj
          <br>
           DB :: Disk usage on /dev/sda1 lksdfjdlfsa jdklsjf kldsaj fldasjfldksj fldskjafdskljfdsalk fjdslkdfajsdsf lkdsfjdlksafj dklsfja dklasj fdasj dlksaj fdlksjadsfj dsfaj dfslkdfjsad flasjdafs ljfdsakl dfjskldafs kldfs jdlfsa jfdsa
          <br>
          <br>
           DB :: Disk usage on /dev/sda1 lksdfjdlfsa jdklsjf kldsaj fldasjfldksj
          <br>
          DB :: Disk usage on /dev/sda1 lksdfjdlfsa jdklsjf kldsaj fldasjfldksj"]]]])


(defn- page
  []
  (html5
   (html-head)
   (html-body)))

(defn handler
  [request]
  {:body (page)
   :status 200})
