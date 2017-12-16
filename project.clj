(defproject mee6 "0.1.0-SNAPSHOT"
  :description "A simple monitoring tool"
  :url ""
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/core.async "0.3.465"]

                 [com.cognitect/transit-clj "0.8.300"]
                 [com.draines/postal "2.0.2" :exclusions [commons-codec]]
                 [com.taoensso/timbre "4.10.0" :exclusions [org.clojure/tools.reader]]
                 [commons-codec/commons-codec "1.11"]

                 [io.forward/yaml "1.0.6"]
                 [org.slf4j/slf4j-nop "1.7.25"]
                 [org.quartz-scheduler/quartz "2.3.0"]
                 [org.quartz-scheduler/quartz-jobs "2.3.0"]

                 [funcool/datoteka "1.0.0"]
                 [funcool/cuerdas "2.0.4"]

                 [cheshire "5.8.0"]
                 [ring "1.6.3"]
                 [mount "0.1.11"]
                 [environ "1.1.0"]
                 [hiccup "1.0.5"]]

  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :profiles
  {:dev {:main ^:skip-aot mee6.repl
         :plugins [[lein-ancient "0.6.15"]]}
   :prod {:main ^:skip-aot mee6.core}
   :uberjar {:main mee6.core :aot :all}
   })
