(ns contacts.dev
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [cljs-http.client :as http]
            [cljs.pprint :as pprint :refer [pprint]]))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))

(defn log [x]
  (println) ;; flush past prompt
  (pprint x))

(comment
  (let [c (http/post "http://localhost:8081/contacts"
            {:transit-params
             {:selector [:person/last-name :person/first-name
                         {:person/telephone [:telephone/number]}]}})]
    (go (log (:body (<! c)))))
  )