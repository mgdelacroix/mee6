(ns hal.modules.disk-usage
  "Disk usage monitoring module."
  (:require [clojure.spec.alpha :as s]
            [cuerdas.core :as str]
            [hal.ssh :as ssh]))

;; --- Spec

(s/def ::device string?)
(s/def ::threshold int?)
(s/def ::contex
  (s/keys :req-un [::device ::threshold]))

;; --- Impl

(declare humanize)

(defn- process-output
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
             (format)
             (humanize))))

(defn humanize
  "Return human readable output."
  [{:keys [capacity used percentage] :as output}]
  (let [capacity (format "%.2f GB" (double (/ capacity 1024 1024)))
        used (format "%.2f GB" (double (/ used 1024 1024)))
        percentage (format "%d%%" percentage)]
    (assoc output :humanized {:capacity capacity
                              :used used
                              :percentage percentage})))


;; --- API

(defn run
  [{:keys [host device] :as ctx}]
  (let [{:keys [exit out] :as output} (ssh/run host "df -l")]
    (if (= exit 0)
      (if-let [output (process-output ctx out)]
        output
        (ex-info (str "Couldn't find device " device) {}))
      (ex-info "Command returned non-zero status" output))))

(defn check
  "Check if the output is an error or not, and if the returned
  information triggers any notification"
  [{:keys [threshold] :as ctx} {:keys [percentage] :as output}]
  (if (> percentage threshold)
    :red
    :green))

