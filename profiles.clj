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
                 [bk/ring-gzip "0.2.1"]

                 [compojure "1.6.0" :exclusions [ring/ring-core]]
                 [commons-codec "1.11"]
                 [cheshire "5.8.0"]
                 [hiccup "1.0.5"]
                 [environ "1.1.0"]
                 [ring "1.6.3"]
                 [ring-cors "0.1.11"]
                 [mount "0.1.11"]
                 [docopt "0.6.1"]]
  :source-paths ^:replace ["src/back"]}

 :front
 {:dependencies [[org.clojure/clojurescript "1.9.946"]
                 [funcool/rumext "1.1.0"]
                 [rum "0.10.8" :exclusions [sablono]]
                 [cljsjs/date-fns "1.29.0-0"]
                 [sablono "0.8.0"]

                 [funcool/beicon "4.1.0"]
                 [funcool/rxhttp "1.0.0-SNAPSHOT"]
                 [funcool/bide "1.6.0"]
                 [funcool/cuerdas "2.0.4"]
                 [funcool/lentes "1.2.0"]
                 [funcool/potok "2.3.0"]]
  :plugins [[lein-figwheel "0.5.14"]
            [lein-cljsbuild "1.1.7"]]
  :source-paths ^:replace ["src/front"]
  :figwheel {:css-dirs ["resources/public"]}
  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src/front"]
             :figwheel {:on-jsload "mee6.main/init"}
             :compiler {:main "mee6.main"
                        :closure-defines {"mee6.config.url" "http://localhost:3001/graphql"}
                        :cache-analysis false
                        :parallel-build false
                        :optimizations :none
                        :language-in  :ecmascript5
                        :language-out :ecmascript5
                        :source-map true
                        :output-to "resources/public/js/main.js"
                        :output-dir "resources/public/js/main-dev"
                        :asset-path "/js/main-dev"
                        :verbose true}}
            {:id "prod"
             :source-paths ["src/front"]
             :figwheel false
             :compiler {:main "mee6.main"
                        :closure-defines {"mee6.config.url" "graphql"}
                        :cache-analysis false
                        :parallel-build false
                        :pretty-print false
                        :pseudo-names false
                        :optimizations :simple
                        :fn-invoke-direct false
                        :static-fns true
                        :language-in  :ecmascript5
                        :language-out :ecmascript5
                        :source-map "resources/public/js/main.js.map"
                        :output-to "resources/public/js/main.js"
                        :output-dir "resources/public/js/main"
                        :asset-path "/js/main"
                        :verbose true}}]}
  :clean-targets ^{:protect false} ["resources/public/js"]}
 :dev {:main ^:skip-aot mee6.repl
       :plugins [[lein-ancient "0.6.15"]]}
 :prod [:back {:main ^:skip-aot mee6.core}]
 :uberjar [:back {:main mee6.core :aot :all}]}
