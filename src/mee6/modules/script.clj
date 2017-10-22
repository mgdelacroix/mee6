(ns mee6.modules.script
  (:require [mee6.ssh :as ssh]
            [mee6.modules :as mod]
            [datoteka.core :as fs]
            [cuerdas.core :as str]))

(defn- get-remote-path
  [local-path]
  (let [filename (fs/name local-path)]
    (str "/tmp/.mee6-" filename)))

(defn- parse-kvline
  [state line]
  (let [[key value] (map str/trim (str/split line #":" 2))]
    (assoc! state (keyword "script" key) value)))

(defn- parse-kvlines
  [kvlines]
  (persistent!
   (reduce parse-kvline (transient {}) kvlines)))

(defn process-script-output
  [output]
  (let [[stdout kvpairs] (split-with #(not= % "---") (str/lines output))]
    (-> (parse-kvlines (rest kvpairs))
        (assoc :stdout (str/join "\n" stdout)))))

(defn instance
  [{:keys [host file args] :as ctx}]
  (reify
    mod/IModule
    (-run [_ local]
      (let [remote-path (get-remote-path file)
            _ (ssh/copy host file remote-path)
            result (ssh/run host (str remote-path " " args))]
        (-> (process-script-output (:out result))
            (assoc :exitcode (:exit result)))))

    (-check [_ local]
      (let [{:keys [exitcode]} local]
        (case exitcode
          0 :green
          1 :red
          :grey)))))
