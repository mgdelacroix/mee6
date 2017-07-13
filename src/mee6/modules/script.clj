(ns mee6.modules.script
  (:require [mee6.ssh :as ssh]
            [clojure.java.io :as io]
            [cuerdas.core :as str]))

(defn get-basename
  [path]
  (-> (io/file path)
      (.getName)))

(defn get-remote-path
  [local-path]
  (let [filename (get-basename local-path)]
    (str "/tmp/.mee6-" filename)))

(defn process-out
  [{:keys [exit out] :as result}]
  (letfn [(reduce-output [acc val]
            (if (or (= val "---") (not (empty? acc)))
              (conj acc val)
              acc))
          (transform-to-map [acc val]
            (let [[key value] (map str/trim (str/split val #":" 2))
                  key (keyword "script" key)]
              (assoc acc key value)))]
    (->> (str/lines out)
         (reduce reduce-output [])
         (rest)
         (reduce transform-to-map result))))

(defn run
  [{:keys [host file args] :as ctx}]
  (let [remote-path (get-remote-path file)
        _ (ssh/copy host file remote-path)
        {:keys [exit] :as result} (ssh/run host (str remote-path " " args))]
    (process-out result)))

(defn check
  [ctx {:keys [exit out err] :as result}]
  (case exit
    0 :green
    1 :red
    :grey))
