(defproject budget "0.1.0-SNAPSHOT"
  :description "Fun budgeting program"
  :url "https://github.com/lsund/budget"


  :min-lein-version "2.7.0"

  :dependencies [;; Commons
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async  "0.4.474"]
                 [org.clojure/tools.namespace "0.2.11"]

                 ;; Web
                 [reagent "0.8.0"]
                 [com.taoensso/sente "1.12.0"]
                 [http-kit "2.2.0"]
                 [hiccup "1.0.5"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.0"]

                 ,,,]

  :plugins [[lein-figwheel "0.5.15"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src/clj src/cljs"]

  :ring
  {:handler budget.core/new-handler}

  :main
  budget.main

  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src/cljs"]

                :figwheel {:on-jsload "budget.core/on-js-reload"
                           :open-urls ["http://localhost:3449/index.html"]}

                :compiler {:main                 budget.core
                           :asset-path           "js/compiled/out"
                           :output-to            "resources/public/js/compiled/budget.js"
                           :output-dir           "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads             [devtools.preload]}}
               {:id           "min"
                :source-paths ["src/cljs"]
                :compiler     {:output-to     "resources/public/js/compiled/budget.js"
                               :main          budget.core
                               :optimizations :advanced
                               :pretty-print  false}}]}

  :figwheel
  {:css-dirs ["resources/public/css"]}

  :repl-options
  {:init-ns user}

  :profiles {:dev {:dependencies  [[binaryage/devtools "0.9.9"]
                                   [figwheel-sidecar "0.5.15"]
                                   [com.cemerick/piggieback "0.2.2"]]
                   :source-paths  ["src/clj" "src/cljs" "dev"]
                   :repl-options  {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}})
