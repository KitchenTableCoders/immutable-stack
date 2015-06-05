(ns contacts.dev
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [cljs-http.client :as http]
            [cljs.pprint :as pprint :refer [pprint]]
            [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))
