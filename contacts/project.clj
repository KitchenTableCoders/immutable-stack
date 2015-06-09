(defproject contacts "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ^:replace ["-Xms512m" "-Xmx512m" "-server"]

  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/clojurescript "0.0-3311"]
                 [com.datomic/datomic-free "0.9.5153"]
                 [bidi "1.10.2"]
                 [org.omcljs/om "0.9.0-SNAPSHOT"]
                 [ring/ring "1.2.2"]
                 [com.cognitect/transit-clj "0.8.271"]
                 [com.cognitect/transit-cljs "0.8.220"]
                 [cljs-http "0.1.30" :exclusions
                  [org.clojure/clojure org.clojure/clojurescript
                   com.cognitect/transit-cljs]]
                 [cljsjs/codemirror "5.1.0-2"]
                 [com.stuartsierra/component "0.2.1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.0.2"]
            [lein-ring "0.8.10"]
            [lein-beanstalk "0.2.7"]]

  :ring {:handler contacts.core/service
         :init contacts.core/start
         :destroy contacts.core/stop}

  :cljsbuild {
    :builds [
      {:id "dev"
       :source-paths ["src/cljs"]
       :compiler {
         :output-to "resources/public/js/main.js"
         :output-dir "resources/public/js/out"
         :optimizations :none
         :source-map true
       }}
      {:id "release"
       :source-paths ["src/cljs"]
       :compiler {
         :output-to "resources/public/js/main.js"
         :optimizations :advanced
         :output-wrapper true
         :pretty-print false
         :preamble ["react/react.min.js"]
         :externs ["react/externs/react.js"]
       }}
    ]
  })
