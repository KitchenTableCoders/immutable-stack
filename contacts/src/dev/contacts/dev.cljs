(ns contacts.dev
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [ajax.core :refer [POST]]
            [cljs-http.client :as http]))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))

(comment
  (let [c (http/post "http://localhost:8081/contacts"
            {:transit-params
             {:selector [:person/last-name :person/first-name
                         {:person/telephone [:telephone/number]}]}})]
    (go (println (:body (<! c)))))
  )