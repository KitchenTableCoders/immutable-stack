(ns contacts.core
  (:require [com.stuartsierra.component :as component]
            [contacts.system :as system]))

(def sys (system/system {:db-uri   "datomic:mem://localhost:4334/contacts"
                         :web-port 8080}))

;; just a work around for LT, can't start at top level for some reason
(defn start []
  (component/start sys))

(start)
