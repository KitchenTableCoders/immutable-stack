(ns contacts.dev
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [contacts.demo1]
            [contacts.demo2]))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))
