(ns contacts.system
  (:require [com.stuartsierra.component :as component]
            contacts.server
            [contacts.datomic :as contacts]))

(defn dev-system [config-options]
  (let [{:keys [db-uri web-port]} config-options]
    (component/system-map
      :db (contacts.datomic/new-database db-uri)
      :webserver
      (component/using
        (contacts.server/dev-server web-port)
        {:datomic-connection  :db}))))

(defn prod-system [config-options]
  (let [{:keys [db-uri]} config-options]
    (component/system-map
      :db (contacts.datomic/new-database db-uri)
      :webserver
      (component/using
        (contacts.server/prod-server)
        {:datomic-connection  :db}))))

(comment
  (def s (dev-system {:db-uri   "datomic:mem://localhost:4334/contacts"
                      :web-port 8081}))
  (def s1 (component/start s))

  (require '[datomic.api :as d])

  (def conn (-> s1 :db :connection))
  (def db (d/db conn))

  (d/transact conn (read-string (slurp "resources/data/initial.edn")))

  (let [db (d/db conn)]
    (d/pull db [:person/first-name :person/last-name {:person/telephone [:telephone/number]}]
      17592186045423))

  (contacts/list-contacts db
    [:person/first-name :person/last-name {:person/telephone [:telephone/number]}])
)
