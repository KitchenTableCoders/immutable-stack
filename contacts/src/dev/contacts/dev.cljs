(ns contacts.dev
  (:require [clojure.browser.repl :as repl]
            [ajax.core :refer [GET]]))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))

(defn get-contacts [contacts]
  (println contacts))

(defn get-contacts-error [err]
  (println err))

(comment
  (GET "http://localhost:8081/contacts"
    {:handler get-contacts
     :error-handler get-contacts-error})
  )