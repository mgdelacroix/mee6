(ns mee6.http
  (:require [clojure.core.async :as a]
            [clojure.java.io :as io]
            [clojure.tools.trace :refer [trace]]
            [cuerdas.core :as str]
            [cheshire.core :as json]
            [mount.core :refer [defstate]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.util.response :refer [resource-response]]
            [ring.util.io :refer [piped-input-stream]]
            [mee6.services :as sv]
            [mee6.config :as cfg]
            [mee6.database :as db]
            [mee6.graphql :as gql]
            [mee6.util.template :as tmpl]
            [mee6.util.logging :as log])
  (:import [java.io IOException OutputStream Writer]))

;; --- Router

(def not-found
  (constantly {:status 404 :body "not found"}))

(defn handle-graphiql
  [req]
  (let [encode #(-> (json/encode %) (str/replace #"\/" "\\/"))
        params {:query (encode (get-in req [:params :query] ""))
                :variables (encode (get-in req [:params :variables] ""))
                :operation (encode (get-in req [:params :operationName] ""))}
        body (tmpl/render "graphiql.html" params)]
    (if (get-in cfg/config [:http :graphiql])
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body body}
      (not-found req))))

(defn handle-graphql
  [req]
  (let [rsp (atom {})
        context {:req req :rsp rsp :authenticated (:authenticated req)}
        query (get-in req [:body :query])
        params (get-in req [:body :variables])
        result (gql/execute query params context)]
    (merge @rsp
           {:status 200
            :headers {"Content-Type" "application/json"}
            :body (json/encode result)})))

(defn handle-sse
  [req]
  (letfn [(write-message [writer message]
            (try
              (doto writer
                (.write message)
                (.flush))
              true
              (catch IOException e
                (.printStackTrace e)
                (println e)

                false)))

          (sse-loop [^OutputStream out]
            (try
              (let [inbox (a/chan 10 (map :payload))
                    writer (io/make-writer out {})]
                (a/go []
                  (println "start inner loop")
                  (dotimes [i 5]
                    (println "inner loop generate message")
                    (a/>! inbox {:payload {:counter i}}))
                  #_(a/close! inbox))
                ;; (sv/sub! :notifications inbox)
                (loop []
                  (let [[message port] (a/alts!! [inbox (a/timeout 1000)])]
                    (let [message (json/encode message)
                          buffer  (str/istr "event: message\n"
                                            "data: ~{message}\n\n")]
                      (if (write-message writer buffer)
                        (recur)
                        (a/close! inbox))))))
              (catch Throwable e
                (println e)

                (.printStackTrace e))))]

    {:headers {"content-type" "text/event-stream;charset=UTF-8"
               "cache-control" "no-cache"
               "transfer-encoding" "chunked"}
     :status 200
     :body (piped-input-stream sse-loop)}))

(defroutes app
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (POST "/graphql" request (handle-graphql request))
  (GET  "/graphiql" request (handle-graphiql request))
  (GET  "/sse" request (handle-sse request))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(defn wrap-auth
  [handler]
  (letfn [(valid-token? [token]
            (not (nil? (get-in @db/state [:tokens token]))))]
    (fn [{:keys [cookies] :as request}]
      (let [token (get-in cookies ["auth-token" :value])]
        (if (and token (not (nil? (get-in @db/state [:tokens token]))))
          (handler (assoc request :authenticated true))
          (handler (assoc request :authenticated false)))))))

(defn wrap-errors
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (.printStackTrace e)
        (throw e)))))

;; --- API

(def ^:private defaults
  {:port 3000
   :daemon? false
   :join? false})

(defn- start
  [{:keys [http] :as config}]
  (when http
    (log/inf "Starting http server on port" (:port http))
    (let [options (merge defaults http)]
      (-> app
          (wrap-errors)
          (wrap-cors :access-control-allow-origin [#".*"]
                     :access-control-allow-credentials "true"
                     :access-control-allow-methods [:get :post :options]
                     :access-control-allow-headers [:x-requested-with :content-type :authorization])
          (wrap-auth)
          (wrap-keyword-params)
          (wrap-params)
          (wrap-cookies)
          (wrap-json-body {:keywords? true})
          (wrap-json-response)
          (wrap-gzip)
          (wrap-errors)
          (jetty/run-jetty options)))))

(defn- stop
  [server]
  (when server
    (.stop server)))

(defstate server
  :start (start cfg/config)
  :stop (stop server))
