(defproject contacts "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  ;:exclusions
  ;[org.clojure/google-closure-library-third-party]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [com.datomic/datomic-free "0.9.4699"]
                 [bidi "1.10.2"]
                 [om "0.5.3"]
                 [secretary "1.1.0"]
                 [ring/ring "1.2.2"]
                 [fogus/ring-edn "0.2.0"]
                 [com.stuartsierra/component "0.2.1"]]

  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.0.2"]]

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
         :output-to "reources/public/js/main.js"
         :optimizations :advanced
         :output-wrapper true
         :pretty-print false
       }}
    ]
  })
