(require '[cljs.build.api :as b])
(require '[cljs.repl :as repl])
(require '[cljs.repl.browser :as browser])

(b/build (b/inputs "src/dev")
  {:main 'contacts.dev
   :asset-path "js"
   :output-to "resources/public/js/app.js"
   :output-dir "resources/public/js"})

(repl/repl
  (browser/repl-env)
  :output-dir "resources/public/js")