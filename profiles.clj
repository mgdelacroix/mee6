{:back
 {:dependencies [[org.clojure/tools.namespace "0.2.11"]
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
  :source-paths ["src/back"]}

 :front
 {:dependencies [[org.clojure/clojurescript "1.9.946"]
                 [rum "0.10.8" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [cljsjs/react "15.6.2-2"]
                 [cljsjs/react-dom "15.6.2-2"]
                 [cljsjs/react-dom-server "15.6.2-2"]

                 [funcool/beicon "4.1.0"]
                 [funcool/bide "1.6.0"]
                 [funcool/cuerdas "2.0.4"]
                 [funcool/lentes "1.2.0"]
                 [funcool/potok "2.3.0"]]
  :plugins [[lein-figwheel "0.5.14"]]
  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src/front"]
             :figwheel true
             :compiler {:main "mee6.main"
                        :cache-analysis false
                        :parallel-build false
                        :optimizations :none
                        :language-in  :ecmascript6
                        :language-out :ecmascript5
                        :output-to "resources/public/js/main.js"
                        :output-dir "resources/public/js/main"
                        :asset-path "/js/main"
                        :verbose true}}]}
  :clean-targets ["resources/public/js"]}

 :dev {:main ^:skip-aot mee6.repl
       :plugins [[lein-ancient "0.6.15"]]}
 :prod {:main ^:skip-aot mee6.core}
 :uberjar {:main mee6.core :aot :all}}
