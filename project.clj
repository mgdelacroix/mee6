(defproject mee6 "0.1.0-SNAPSHOT"
  :description "A simple monitoring tool"
  :url ""
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha19"]
                 [org.clojure/tools.namespace "0.2.11"]

                 [com.taoensso/timbre "4.10.0"]
                 [org.slf4j/slf4j-nop "1.7.25"]
                 [org.quartz-scheduler/quartz "2.3.0"]
                 [org.quartz-scheduler/quartz-jobs "2.3.0"]
                 [io.forward/yaml "1.0.6"]
                 [ring "1.6.2"]
                 [mount "0.1.11"]
                 [environ "1.1.0"]
                 [funcool/cuerdas "2.0.3"]
                 [com.draines/postal "2.0.2"]
                 [hiccup "1.0.5"]]

  :javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]
  :profiles
  {:dev {:main ^:skip-aot mee6.repl
         :plugins [[lein-ancient "0.6.10"]]}
   :prod {:main ^:skip-aot mee6.core}
   :uberjar {:main mee6.core :aot :all}
   })
