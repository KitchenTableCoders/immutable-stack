(defproject contacts "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [com.datomic/datomic-free "0.9.4699"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :cljsbuild {
    :builds [
      {:id "dev"
       :source-paths ["src/cljs"]
       :compiler {
         :output-to "resources/public/js/main.js"
         :output-dir "resoures/public/js/out"
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