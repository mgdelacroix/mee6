(defproject hal "0.1.0-SNAPSHOT"
  :description "A simple monitoring tool"
  :url ""
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [io.forward/yaml "1.0.6"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [twarc "0.1.10"]]
  :profiles {:dev {:main ^:skip-aot hal.repl}
             :prod {:main ^:skip-aot hal.core}})
