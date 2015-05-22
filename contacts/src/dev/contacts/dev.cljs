(ns contacts.dev
  (:require [clojure.browser.repl :as repl]
            [ajax.core :refer [GET]]))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))
