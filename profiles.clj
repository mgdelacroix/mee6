{:back
 {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/core.async "0.3.465"]

                 [com.cognitect/transit-clj "0.8.300"]
                 [com.draines/postal "2.0.2" :exclusions [commons-codec]]
                 [com.github.spullara.mustache.java/compiler "0.9.4"]
                 [com.taoensso/timbre "4.10.0" :exclusions [org.clojure/tools.reader]]
                 [com.walmartlabs/lacinia "0.23.0" :exclusions [clojure-future-spec]]

                 [io.forward/yaml "1.0.6"]
                 [org.quartz-scheduler/quartz "2.3.0"]
                 [org.quartz-scheduler/quartz-jobs "2.3.0"]
                 [org.slf4j/slf4j-nop "1.7.25"]

                 [funcool/cuerdas "2.0.4"]
                 [funcool/datoteka "1.0.0"]
                 [ring/ring-json "0.4.0" :exclusions [ring/ring-core]]

                 [commons-codec "1.11"]
                 [cheshire "5.8.0"]
                 [hiccup "1.0.5"]
                 [environ "1.1.0"]
                 [ring "1.6.3"]
                 [mount "0.1.11"]]
  :source-paths ^:replace ["src/back"]}

 :front
 {:dependencies [[org.clojure/clojurescript "1.9.946"]
                 [funcool/rumext "1.1.0"]
                 [rum "0.10.8" :exclusions [sablono]]
                 [sablono "0.8.0"]

                 [funcool/beicon "4.1.0"]
                 [funcool/bide "1.6.0"]
                 [funcool/cuerdas "2.0.4"]
                 [funcool/lentes "1.2.0"]
                 [funcool/potok "2.3.0"]]
  :plugins [[lein-figwheel "0.5.14"]]
  :source-paths ^:replace ["src/front"]
  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src/front"]
             :figwheel {:on-jsload "mee6.main/init"}
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
  :clean-targets ^{:protect false} ["resources/public/js"]}

 :dev {:main ^:skip-aot mee6.repl
       :plugins [[lein-ancient "0.6.15"]]}
 :prod {:main ^:skip-aot mee6.core}
 :uberjar {:main mee6.core :aot :all}}
