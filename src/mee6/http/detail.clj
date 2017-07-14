(ns mee6.http.detail
  (:require [clojure.java.io :as io]
            [cuerdas.core :as str]
            [clojure.pprint :refer [pprint]]
            [hiccup.page :refer [html5]]
            [yaml.core :as yaml]
            [mee6.uuid :as uuid]
            [mee6.state :refer [state]]
            [mee6.http.common :refer [html-head body-header]]
            [mee6.http.home :refer [body-content-summary body-content-item]]))

(defn dump-yaml
  [value]
  (yaml/generate-string value :dumper-options {:flow-style :block}))

(defn get-check
  [checks id]
  (->> checks
       (filter #(= id (:id %)))
       (first)))

(defn body-content
  [{:keys [checks results] :as state} id]
  (let [{:keys [name host cron] :as check} (get-check checks id)
        {:keys [status output updated-at] :as result} (get results id)
        special-out (select-keys output [:out :err :exit])
        normal-out (if (:humanized output)
                     (:humanized output)
                     (dissoc output :out :err :exit))]

    [:div#main-content.content
     [:section#items
      (body-content-summary state)

      [:ul.list.items
       (body-content-item check result)]

      [:h3 "Latest output:"]
      [:section.code
       [:pre (dump-yaml normal-out)]]

      (let [out (:out special-out)]
        (if (and (string? out) (not (str/empty? out)))
          [:div
           [:h3 "Output:"]
           [:section.code
            [:pre out]]]))

      (let [err (:err special-out)]
        (if (and (string? err) (not (str/empty? err)))
          [:div
           [:h3 "Error:"]
           [:section.code
            [:pre err]]]))

      [:h3 "Check configuration:"]
      [:section.code
       [:pre (dump-yaml (dissoc check :hosts :id))]]]]))

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
    {:body (page state id)
     :status 200}))
