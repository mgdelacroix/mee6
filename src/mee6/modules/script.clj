(ns mee6.modules.script
  (:require [clojure.java.io :as io]
            [mee6.cmd :as cmd]
            [mee6.modules :as mod]
            [datoteka.core :as fs]
            [cuerdas.core :as str]))

(defn- parse-kvline
  [state line]
  (let [[key value] (map str/trim (str/split line #":" 2))]
    (assoc! state (keyword "script" key) value)))

(defn- parse-kvlines
  [kvlines]
  (persistent!
   (reduce parse-kvline (transient {}) kvlines)))

(defn- process-script-output
  [output]
  (let [[stdout kvlines] (split-with #(not= % "---") (str/lines output))]
    {:stdout (str/join "\n" stdout)
     :kvpairs (parse-kvlines (rest kvlines))}))

(defn- run
  [{:keys [host file args] :as ctx} local]
  (let [result (cmd/run-user-script host (slurp (io/file file)))]
    (-> (:out result)
        (process-script-output)
        (assoc :exitcode (:exit result)))))

(defn- check
  [ctx {:keys [exitcode] :as local}]
  (let [{:keys [exitcode]} local]
    (case exitcode
      0 :green
      1 :red
      :grey)))

(defn instance
  [ctx]
  (reify
    mod/IModule
    (-run [_ local] (run ctx local))
    (-check [_ local] (check ctx local))))
