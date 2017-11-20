(ns mee6.http.detail
  (:require [clojure.java.io :as io]
            [cuerdas.core :as str]
            [clojure.pprint :refer [pprint]]
            [hiccup.page :refer [html5]]
            [yaml.core :as yaml]
            [mee6.engine :as engine]
            [mee6.database :refer [state]]
            [mee6.http.common :refer [html-head body-header]]
            [mee6.http.home :refer [body-content-summary body-content-item]]))

(defn dump-yaml
  [value]
  (yaml/generate-string value :dumper-options {:flow-style :block}))

(defmulti render-check
  (fn [check data] (:module check)))

(defmethod render-check :default
  [check local]
  [:div
   [:h3 "Latest output:"]
   [:section.code
    [:pre (dump-yaml local)]]])

(defmethod render-check "disk-usage"
  [check {:keys [capacity used] :as local}]
  (let [percentage (format "%d%%" (quot (* used 100) capacity))
        capacity (format "%.2f GB" (double (/ capacity 1024 1024)))
        used (format "%.2f GB" (double (/ used 1024 1024)))]
    [:div
     [:h3 "Latest output:"]
     [:section.code
      [:pre (dump-yaml {:capacity capacity
                        :used used
                        :percentage percentage})]]]))


(defmethod render-check "service"
  [check local]
  [:div
   [:h3 "Latest status:"]
   [:section.code
    [:pre (dump-yaml (dissoc local :lastlog))]]

   [:h3 "Last log:"]
   [:section.code
    [:pre (:lastlog local)]]])

(defmethod render-check "script"
  [check {:keys [kvpairs stdout] :as local}]
  [:div
   (when kvpairs
     [:div
      [:h3 "Parsed key-value data:"]
      [:section.code
       [:pre (dump-yaml kvpairs)]]])
   [:h3 "Stdout:"
    [:section.code
     [:pre stdout]]]])

(defn get-check-by-id
  [id]
  (->> engine/checks
       (filter #(= id (:id %)))
       (first)))

(defn body-content
  [state id]
  (let [{:keys [name host cron] :as check} (get-check-by-id id)
        {:keys [status local updated-at error] :as data} (get-in state [:checks id])

        special-out (select-keys local [:out :err :exit])
        normal-out (if (:humanized local)
                     (:humanized local)
                     (dissoc local :out :err :exit))]

    (println check)
    [:div#main-content.content
     [:section#items
      (body-content-summary state)

      [:ul.list.items
       (body-content-item check data)]

      (when local
        (render-check check local))

      (when-let [{:keys [stacktrace]} error]
        [:div
         [:h3 "Error:"]
         [:section.code
          (if stacktrace
            [:pre (:stacktrace error)]
            [:pre (dump-yaml error)])]])

      [:h3 "Check configuration:"]
      [:section.code
       [:pre (dump-yaml (dissoc check :hosts :id))]]]]))

(defn- render-page
  [state id]
  (html5
   (html-head :title "Mee6")
   [:body
    (body-header state)
    (body-content state id)]))

(defn handler
  [{:keys [uri matches] :as request}]
  (try
    (let [[uri id] matches]
      {:body (render-page @state id)
       :status 200})
    (catch Throwable e
      (.printStackTrace e)
      (throw e))))
