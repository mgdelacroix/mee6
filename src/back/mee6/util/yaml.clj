(ns mee6.util.yaml
  (:require [yaml.core :as yaml]))

(defn encode
  [data]
  (yaml/generate-string data :dumper-options {:flow-style :block}))

(defn decode
  [data]
  (yaml/parse-string data))

(defn decode-from-file
  [path]
  (yaml/from-file path))
