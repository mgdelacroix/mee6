(ns mee6.cmd
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [cuerdas.core :as str]))

(defn run-user-script
  [{:keys [uri] :as host} script]
  (let [command (str/istr "cat > /tmp/.mee6_user_script <<EOF\n~{script}\nEOF\n\n"
                          "bash /tmp/.mee6_user_script")]
    (if (= host :mee6.engine/localhost)
      (shell/sh "bash" "-c" command)
      (shell/sh "timeout" "5" "ssh" "-q" uri command))))

(defn run-script
  "Executes a local script by name on remote host."
  [{:keys [uri] :as host} script params]
  (let [content (slurp (io/resource (str "scripts/" script ".py")))
        params (json/encode params)
        command (str/istr "cat > /tmp/~{script}.py <<EOF\n~{content}\nEOF\n\n"
                          "python3 /tmp/~{script}.py '~{params}'")]
    (if (= host :mee6.engine/localhost)
      (shell/sh "bash" "-c" command)
      (shell/sh "timeout" "5" "ssh" "-q" uri command))))
