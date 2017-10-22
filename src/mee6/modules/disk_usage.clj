(ns mee6.modules.disk-usage
  "Disk usage monitoring module."
  (:require [clojure.spec.alpha :as s]
            [cuerdas.core :as str]
            [mee6.modules :as mod]
            [mee6.ssh :as ssh]))

(s/def ::device string?)
(s/def ::threshold int?)
(s/def ::contex
  (s/keys :req-un [::device ::threshold]))

(defn- process-output
  "Process the df command output."
  [{:keys [device threshold] :as ctx} lines]
  {:pre [(s/valid? ::contex ctx)]}
  (letfn [(parse-line [line]
            (let [parts (str/split line #"\s+")]
              (when (>= (count parts) 3)
                [(nth parts 1)
                 (nth parts 2)])))
          (parse-data [[capacity used]]
            (let [capacity (read-string capacity)
                  used (read-string used)]
              {:capacity capacity
               :used used}))
          (match-device? [line]
            (str/starts-with? line device))]
    (some->> (str/lines lines)
             (filter match-device?)
             (first)
             (parse-line)
             (parse-data))))

(defn- retrieve-stats
  [{:keys [host device] :as ctx}]
  (let [{:keys [exit out] :as output} (ssh/run host "df -l")]
    (if (= exit 0)
      (if-let [result (process-output ctx out)]
        result
        (throw (ex-info "Could not find device." {:device device})))
      (throw (ex-info "Command returned non-zero status" {})))))

(defn instance
  [{:keys [threshold] :as ctx}]
  (reify
    mod/IModule
    (-run [_ state]
      (merge state (retrieve-stats ctx)))

    (-check [_ state]
      (let [{:keys [used capacity]} state
            percentage (quot (* used 100) capacity)]
        (if (> percentage threshold)
          :red
          :green)))))
