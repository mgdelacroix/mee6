(ns hal.modules.disk-usage
  (:require [hal.ssh :as ssh]
            [cuerdas.core :as str]))

(defn- error?
  [{:keys [exit]}]
  (not= exit 0))

(defn- match-device?
  [device line]
  (str/starts-with? line device))

(defn- parse-df-line
  [line]
  (println "parse-df-line:" line)
  (let [[_ capacity-str used-str] (str/split line #"\s+")
        capacity (read-string capacity-str)
        used (read-string used-str)
        percentage (quot (* used 100) capacity)]
    {:capacity capacity
     :used used
     :percentage percentage}))

(defn- process-result
  [{:keys [device threshold] :as ctx} {:keys [out] :as result}]
  ;; TODO: add spec for ctx (specific to this module)
  (->> (str/lines out)
       (filter #(match-device? device %))
       (first)
       (parse-df-line)))

(defn run
  [session {:keys [host] :as ctx}]
  (let [result (ssh/run host "df -l")]
    (if (error? result)
      result
      (process-result ctx result))))

(defn check
  "Check if the result is an error or not, and if the returned
  information triggers any notification"
  [{:keys [percentage] :as result} {:keys [threshold] :as ctx} ]
  (if (> percentage threshold)
    :red
    :green))
