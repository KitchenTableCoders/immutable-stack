(ns contacts.datomic
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import datomic.Util))


;; =============================================================================
;; Helpers

;; TODO: remove this and replace with Transit handler

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


;; =============================================================================
;; Queries

(defn list-contacts [db selector]
  (d/q '[:find (pull ?eid selector)
         :in $ selector
         :where
         ;; talk about how we can make it do first OR last name
         [?eid :person/first-name]]
    db selector))

(defn display-contacts [db selector]
  (let [contacts (list-contacts db selector)]
    (map
      #(select-keys % [:db/id :person/last-name :person/first-name])
      (sort-by :person/last-name (map convert-db-id contacts)))))


(defn get-contact [db id-string]
  (convert-db-id (d/touch (d/entity db (edn/read-string id-string)))))


(defn create-contact [conn data]
  (let [tempid (d/tempid :db.part/user)
        r @(d/transact conn [(assoc data :db/id tempid)])]
    (assoc data :db/id (str (d/resolve-tempid (:db-after r) (:tempids r) tempid)))))


(defn update-contact [conn data]
  @(d/transact conn [(assoc data :db/id (edn/read-string (:db/id data)))])
  true)


(defn delete-contact [conn id]
  @(d/transact conn [[:db.fn/retractEntity (edn/read-string id)]])
  true)


;; PHONE
(defn create-phone [conn data]
  (let [tempid (d/tempid :db.part/user)
        r @(d/transact conn
                       [(assoc
                            data
                          :db/id
                          tempid
                          :person/_telephone
                          (edn/read-string (:person/_telephone data)))])]
    (assoc data :db/id (str (d/resolve-tempid (:db-after r) (:tempids r) tempid)))))


(defn update-phone [conn data]
  @(d/transact conn [(assoc data :db/id (edn/read-string (:db/id data)))])
  true)


(defn delete-phone [conn id]
  @(d/transact conn [[:db.fn/retractEntity (edn/read-string id)]])
  true)

;; return datoms to add

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

;; =============================================================================
;; Query testing

(comment
  (create-contact (:connection (:db @contacts.core/servlet-system))
                  {:person/first-name "person" :person/last-name "withphone"})

  (delete-contact (:connection (:db @contacts.core/servlet-system))
                  "17592186045423")

  (update-contact (:connection (:db @contacts.core/servlet-system))
                  {:db/id "17592186045429" :person/first-name "Foooo"})


  (create-phone (:connection (:db @contacts.core/servlet-system))
                  {:person/_telephone "17592186045438"
                   :telephone/number "123-456-7890"})

  (d/touch (d/entity (d/db (:connection (:db @contacts.core/servlet-system)))
             17592186045438))

  (update-phone (:connection (:db @contacts.core/servlet-system))
                {:db/id "17592186045440"
                 :telephone/number "000-456-7890"})

  (delete-phone (:connection (:db @contacts.core/servlet-system))
                "17592186045440")

  )


