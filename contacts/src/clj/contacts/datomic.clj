(ns contacts.datomic
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import datomic.Util))




(defn convert-db-id [x]
  (cond
    (instance? datomic.query.EntityMap x)
    (assoc (into {} (map convert-db-id x))
      :db/id (str (:db/id x)))

    (instance? clojure.lang.MapEntry x)
    [(first x) (convert-db-id (second x))]

    (coll? x)
    (into (empty x) (map convert-db-id x))

    :else x))



(defn list-contacts [db]
  (map
    ;; won't roundtrip to conn bc segment already probably cached
    #(d/entity db (first %))
    (d/q '[:find ?eid
          :where
          ;; talk about how we can make it do first OR last name
          [?eid :person/first-name]]
        db)))

  (defn display-contacts [db]
    (let [contacts (list-contacts db)]
      (map
        #(select-keys % [:db/id :person/last-name :person/first-name])
        (sort-by :person/last-name (map convert-db-id contacts)))))




(defn get-contact [db id-string]
  (convert-db-id (d/touch (d/entity db (edn/read-string id-string)))))




;; return datoms to add
(defn add-person-datoms [db data])



(defn remove-person-datoms [db data])

(def initial-data
  (let [person-id (d/tempid :db.part/user)
        address-id (d/tempid :db.part/user)
        phone-id (d/tempid :db.part/user)
        email-id (d/tempid :db.part/user)]
    [{:db/id person-id
      :person/first-name "Bob"
      :person/last-name  "Smith"
      :person/email      email-id
      :person/telephone phone-id
      :person/address address-id}
     {:db/id email-id
      :email/address     "bob.smith@gmail.com"}
     {:db/id  phone-id
      :telephone/number "123-456-7890"}
     {:db/id address-id
      :address/street "111 Main St"
      :address/city "Brooklyn"
      :address/state "NY"
      :address/zipcode "11234"}]))


(defrecord DatomicDatabase [uri schema initial-data connection]
  component/Lifecycle
  (start [component]
    (d/create-database uri)
    (let [c (d/connect uri)]
      @(d/transact c schema)
      @(d/transact c initial-data)
      (assoc component :connection c)))
  (stop [component]))

(defn new-database [db-uri]
  (DatomicDatabase. db-uri
                    (first (Util/readAll (io/reader (io/resource "data/schema.edn"))))
                    initial-data
                    nil))

