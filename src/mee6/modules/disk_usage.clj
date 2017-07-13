(ns mee6.modules.disk-usage
  "Disk usage monitoring module."
  (:refer-clojure :exclude [format])
  (:require [clojure.spec.alpha :as s]
            [cuerdas.core :as str]
            [mee6.ssh :as ssh]))

;; --- Spec

(s/def ::device string?)
(s/def ::threshold int?)
(s/def ::contex
  (s/keys :req-un [::device ::threshold]))

;; --- Impl

(defn- process-result
  "Process the df command output."
  [{:keys [device threshold] :as ctx} lines]
  {:pre [(s/valid? ::contex ctx)]}
  (letfn [(parse-line [line]
            (let [parts (str/split line #"\s+")]
              (when (>= (count parts) 3)
                [(nth parts 1)
                 (nth parts 2)])))
          (format [[capacity used]]
            (let [capacity (read-string capacity)
                  used (read-string used)
                  percentage (quot (* used 100) capacity)]
              {:capacity capacity
               :used used
               :percentage percentage}))
          (match-device? [line]
            (str/starts-with? line device))]
    (some->> (str/lines lines)
             (filter match-device?)
             (first)
             (parse-line)
             (format))))

;; --- API

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
  (if (> percentage threshold)
    :red
    :green))
