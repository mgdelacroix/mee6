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
  [{:keys [device threshold] :as ctx} lines]
  ;; TODO: add spec for ctx (specific to this module)
  (some->> (str/lines lines)
           (filter #(match-device? device %))
           (first)
           (parse-df-line)))


(defn run
  [{:keys [host device] :as ctx}]
  (let [{:keys [exit out] :as result} (ssh/run host "df -l")]
    (if (= exit 0)
      (if-let [result (process-result ctx out)]
        result
        (ex-info (str "Couldn't find device " device) {}))

      (ex-info "Command returned non-zero status" result))))

(defn check
  "Check if the result is an error or not, and if the returned
  information triggers any notification"
  [{:keys [threshold] :as ctx} {:keys [percentage] :as result}]
  (println "disk_usage:check:" ctx result)
  (if (> percentage threshold)
    :red
    :green))




