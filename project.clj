(defproject hal "0.1.0-SNAPSHOT"
  :description "A simple monitoring tool"
  :url ""
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [io.forward/yaml "1.0.6"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [mount "0.1.11"]
                 [org.slf4j/slf4j-simple "1.7.24"]
                 [org.quartz-scheduler/quartz "2.2.3"]
                 [org.quartz-scheduler/quartz-jobs "2.2.3"]
                 [environ "1.1.0"]
                 [funcool/cuerdas "2.0.3"]]
  :profiles {:dev {:main ^:skip-aot hal.repl}
             :prod {:main ^:skip-aot hal.core}})
