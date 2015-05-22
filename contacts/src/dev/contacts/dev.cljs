(ns contacts.dev
  (:require [clojure.browser.repl :as repl]
            [ajax.core :refer [POST]]))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))

(defn get-contacts [contacts]
  (println contacts))

(defn get-contacts-error [err]
  (println err))

(comment
  (POST "http://localhost:8081/contacts"
    {:handler get-contacts
     :params {:selector [:person/last-name]}
     :error-handler get-contacts-error})
  )