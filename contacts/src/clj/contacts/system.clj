(ns contacts.system
  (:require [com.stuartsierra.component :as component]
            contacts.server
            contacts.datomic))


(defn system [config-options]
  (let [{:keys [db-uri web-port]} config-options]
    (component/system-map
      :db (contacts.datomic/new-database db-uri)
      :webserver
      (component/using
             (contacts.server/web-server web-port)
             {:datomic-connection  :db}))))

(comment
  (def s (system {:db-uri   "datomic:mem://localhost:4334/contacts"
            :web-port 8080}))
  (component/start d))