(ns mee6.modules
  "Dynamic module loading and running."
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [datoteka.core :as fs]
            [cuerdas.core :as str]
            [mee6.exceptions :as exc]
            [mee6.config :as cfg]
            [mee6.uuid :as uuid]
            [mee6.util.yaml :as yaml]
            [mee6.util.crypto :as crypto]))

(s/def ::status #{"green" "red"})
(s/def ::local map?)
(s/def ::script-output-json (s/keys :req-un [::status ::local]))
(s/def ::script-output-string (s/keys :req-un [::status]))

(defn- resolve-from-classpath
  "Respolve module script by name from the classpath."
  [name]
  {:pre [(string? name)]}
  (io/resource (str/istr "scripts/~{name}")))

(defn- resolve-from-userpath
  "Resolve module script by name from the user defined paths."
  [name]
  {:pre [(string? name)]}
  (letfn [(resolve [path]
            (let [path (fs/join path name)]
              (when (fs/regular-file? path)
                (reduced path))))]
    (reduce #(resolve %2) nil (:modules cfg/config []))))

(defn- resolve-script
  "Resolve module script by name."
  [name]
  (let [script (or (resolve-from-classpath name)
                   (resolve-from-userpath name))]
    (if (nil? script)
      (exc/raise :type :engine-error
                 :message (str/istr "Script not found for module '~{name}'."))
      script)))

(defn- run-script
  "Executes a local script by name on remote host."
  [{:keys [uri] :as host} script args]
  (let [content (slurp script)
        tmpname (crypto/digest-data content)
        command (str/istr "cat > /tmp/~{tmpname} <<EOF\n~{content}\nEOF\n\n"
                          "chmod +x /tmp/~{tmpname}\n\n"
                          "/tmp/~{tmpname} ~{args}\n\n")]
    (if (= host :mee6.engine/localhost)
      (shell/sh "bash" "-c" command)
      (shell/sh "timeout" "5" "ssh" "-q" uri command))))

(defn- prepare-kwargs
  [data]
  (letfn [(prepare-key [key]
            (str "--" (name key)))
          (prepare-val [val]
            (if (string? val)
              (-> (str/replace val "'" "\\'")
                  (str/surround "'"))
              (prepare-val (json/encode val))))

          (process-keyval [acc key val]
            (let [pkey (prepare-key key)]
              (if (sequential? val)
                (into acc (->> (map prepare-val val)
                               (map #(vector pkey %))))
                (conj acc [pkey (prepare-val val)]))))]
    (reduce-kv process-keyval [] data)))

(defn- prepare-arguments
  [check local]
  (let [initial (-> (json/encode {:params check :local (or local {})})
                    (str/replace "'" "\\'")
                    (str/surround "'"))
        kwargs  (prepare-kwargs (dissoc check :name :host :tags :cron))]
    (->> (into [initial] (mapcat identity kwargs))
         (str/join " "))))

(defn- parse-string-output
  [output]
  (letfn [(parse-kvline [state line]
            (let [[key value] (map str/trim (str/split line #":" 2))]
              (assoc! state key value)))
          (parse-kvlines [kvlines]
            (persistent!
             (reduce parse-kvline (transient {}) kvlines)))]
    (let [[stdout kvlines] (split-with #(not= % "---") (str/lines output))
          stdout (str/join "\n" stdout)
          data   (-> (parse-kvlines (rest kvlines))
                     (walk/keywordize-keys))
          status (keyword (:status data))
          local  (assoc data :stdout stdout)]
      (when-not (s/valid? ::script-output-string data)
        (exc/raise :type :module-error
                   :message "Output does not conform with mee6's spec."
                   :output (yaml/encode data)
                   :hint (s/explain-str ::script-output-string data)))
      [status local])))

(defn- parse-json-output
  [output]
  (try
    (let [data (json/decode output true)]
      (when-not (s/valid? ::script-output-json data)
        (exc/raise :type :module-error
                   :message "Output does not conform with mee6's spec."
                   :output (yaml/encode data)
                   :hint (s/explain-str ::script-output-json data)))
      [(keyword (:status data)) (:local data)])
    (catch com.fasterxml.jackson.core.JsonParseException e
      ::invalid)))

(defn execute
  "Resolve and execute check."
  [{:keys [module host] :as check} local]
  (let [local (or local {})
        args (prepare-arguments check local)
        script (resolve-script module)
        {:keys [exit out err] :as result} (run-script host script args)]
    (if-not (zero? exit)
      (exc/raise :type :execution-error
                 :message "Error running the script."
                 :stdout out
                 :stderr err)
      (let [result (parse-json-output out)]
        (if (= result ::invalid)
          (parse-string-output out)
          result)))))
