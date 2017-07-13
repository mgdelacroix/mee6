(ns hal.http.detail
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [hiccup.page :refer [html5]]
            [yaml.core :as yaml]
            [hal.uuid :as uuid]
            [hal.state :refer [state]]
            [hal.http.common :refer [html-head body-header]]
            [hal.http.home :refer [body-content-summary body-content-item]]))

(defn get-check
  [checks id]
  (->> checks
       (filter #(= id (:id %)))
       (first)))

(defn body-content
  [{:keys [checks results] :as state} id]
  (let [{:keys [name host cron] :as check} (get-check checks id)
        {:keys [status output updated-at] :as result} (get results id)]
    [:div.content
     [:section#items
      (body-content-summary state)

      [:ul.list.items
       (body-content-item check result)]

      [:h3 "Latest output:"]
      [:section.code
       [:pre (with-out-str (pprint output))]]

      [:h3 "Check configuration:"]
      [:section.code
       [:pre (yaml/generate-string (dissoc check :hosts :id))]]]]))

(defn- page
  [state id]
  (html5
   (html-head :title "Mee6")
   [:body
    (body-header state)
    (body-content state id)]))

(defn handler
  [{:keys [uri matches] :as request}]
  (let [state (deref state)
        [uri sid] matches
        id (uuid/from-string sid)]
    (println "handler" id)
    {:body (page state id)
     :status 200}))
